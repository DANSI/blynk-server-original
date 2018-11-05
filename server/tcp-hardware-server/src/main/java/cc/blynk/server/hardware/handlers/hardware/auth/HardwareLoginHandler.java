package cc.blynk.server.hardware.handlers.hardware.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.hardware.handlers.hardware.HardwareHandler;
import cc.blynk.server.internal.ReregisterChannelUtil;
import cc.blynk.utils.IPUtils;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.structure.LRUCache;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.RejectedExecutionException;

import static cc.blynk.server.core.protocol.enums.Command.CONNECT_REDIRECT;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_CONNECTED;
import static cc.blynk.server.internal.CommonByteBufUtil.invalidToken;
import static cc.blynk.server.internal.CommonByteBufUtil.makeASCIIStringMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.server.internal.CommonByteBufUtil.serverError;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;

/**
 * Handler responsible for managing hardware and apps login messages.
 * Initializes netty channel with a state tied with user.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class HardwareLoginHandler extends SimpleChannelInboundHandler<LoginMessage> {

    private static final Logger log = LogManager.getLogger(HardwareLoginHandler.class);

    private static final int HARDWARE_PIN_MODE_MSG_ID = 1;

    private final Holder holder;
    private final DBManager dbManager;
    private final BlockingIOProcessor blockingIOProcessor;
    private final String listenPort;
    private final boolean allowStoreIp;

    public HardwareLoginHandler(Holder holder, int listenPort) {
        this.holder = holder;
        this.dbManager = holder.dbManager;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        boolean isForce80ForRedirect = holder.props.getBoolProperty("force.port.80.for.redirect");
        this.listenPort = isForce80ForRedirect ? "80" : String.valueOf(listenPort);
        this.allowStoreIp = holder.props.getAllowStoreIp();
    }

    private void completeLogin(Channel channel, Session session, User user,
                                      DashBoard dash, Device device, int msgId) {
        log.debug("completeLogin. {}", channel);

        session.addHardChannel(channel);
        channel.write(ok(msgId));

        String body = dash.buildPMMessage(device.id);
        if (dash.isActive && body != null) {
            channel.write(makeASCIIStringMessage(HARDWARE, HARDWARE_PIN_MODE_MSG_ID, body));
        }

        channel.flush();

        String responseBody = String.valueOf(dash.id) + DEVICE_SEPARATOR + device.id;
        session.sendToApps(HARDWARE_CONNECTED, msgId, dash.id, responseBody);
        log.trace("Connected device id {}, dash id {}", device.id, dash.id);
        device.connected();
        if (device.firstConnectTime == 0) {
            device.firstConnectTime = device.connectTime;
        }
        if (allowStoreIp) {
            device.lastLoggedIP = IPUtils.getIp(channel.remoteAddress());
        }

        log.info("{} hardware joined.", user.email);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) {
        String token = message.body.trim();
        TokenValue tokenValue = holder.tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            //token should always be 32 chars and shouldn't contain invalid nil char
            if (token.length() != 32 || token.contains(StringUtils.BODY_SEPARATOR_STRING)) {
                log.debug("HardwareLogic token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
                ctx.writeAndFlush(invalidToken(message.id), ctx.voidPromise());
            } else {
                //no user on current server, trying to find server that user belongs to.
                checkTokenOnOtherServer(ctx, token, message.id);
            }
            return;
        }

        User user = tokenValue.user;
        Device device = tokenValue.device;
        DashBoard dash = tokenValue.dash;

        if (tokenValue.isTemporary()) {
            holder.tokenManager.updateRegularCache(token, tokenValue);
            user.profile.addDevice(dash, device);
            user.lastModifiedTs = System.currentTimeMillis();
        }

        createSessionAndReregister(ctx, user, dash, device, message.id);
    }

    private void createSessionAndReregister(ChannelHandlerContext ctx,
                                            User user, DashBoard dash, Device device, int msgId) {
        HardwareStateHolder hardwareStateHolder = new HardwareStateHolder(user, dash, device);

        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.replace(this, "HHArdwareHandler", new HardwareHandler(holder, hardwareStateHolder));

        Session session = holder.sessionDao.getOrCreateSessionByUser(
                hardwareStateHolder.userKey, ctx.channel().eventLoop());

        if (session.isSameEventLoop(ctx)) {
            completeLogin(ctx.channel(), session, user, dash, device, msgId);
        } else {
            log.debug("Re registering hard channel. {}", ctx.channel());
            ReregisterChannelUtil.reRegisterChannel(ctx, session, channelFuture ->
                    completeLogin(channelFuture.channel(), session, user, dash, device, msgId));
        }
    }

    private void checkTokenOnOtherServer(ChannelHandlerContext ctx, String token, int msgId) {
        //check cache first
        LRUCache.CacheEntry cacheEntry = LRUCache.LOGIN_TOKENS_CACHE.get(token);
        if (cacheEntry == null) {
            try {
                blockingIOProcessor.executeDBGetServer(() -> {
                    String server;
                    log.debug("Checking invalid token in DB.");
                    server = dbManager.getServerByToken(token);
                    LRUCache.LOGIN_TOKENS_CACHE.put(token, new LRUCache.CacheEntry(server));
                    // no server found, that's means token is wrong.
                    sendRedirectResponse(ctx, token, server, msgId);
                });
            } catch (RejectedExecutionException ree) {
                log.warn("Error in getServerByToken handler. Limit of tasks reached.");
                ctx.writeAndFlush(serverError(msgId), ctx.voidPromise());
            }
        } else {
            log.debug("Taking token from cache.");
            sendRedirectResponse(ctx, token, cacheEntry.value, msgId);
        }
    }

    private void sendRedirectResponse(ChannelHandlerContext ctx, String token, String server, int msgId) {
        MessageBase response;
        if (server == null || server.equals(holder.props.host)) {
            log.debug("HardwareLogic token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            response = invalidToken(msgId);
        } else {
            log.debug("Redirecting token '{}' to {}", token, server);
            response = makeASCIIStringMessage(CONNECT_REDIRECT, msgId, server + BODY_SEPARATOR + listenPort);
        }
        ctx.writeAndFlush(response, ctx.voidPromise());
    }

}

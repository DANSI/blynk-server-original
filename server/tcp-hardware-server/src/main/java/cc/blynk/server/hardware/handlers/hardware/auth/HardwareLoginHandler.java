package cc.blynk.server.hardware.handlers.hardware.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.common.HardwareNotLoggedHandler;
import cc.blynk.server.hardware.handlers.hardware.HardwareHandler;
import cc.blynk.utils.IPUtils;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.structure.LRUCache;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.CONNECT_REDIRECT;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_CONNECTED;
import static cc.blynk.server.internal.BlynkByteBufUtil.invalidToken;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeASCIIStringMessage;
import static cc.blynk.server.internal.BlynkByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

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
public class HardwareLoginHandler extends SimpleChannelInboundHandler<LoginMessage>
        implements DefaultReregisterHandler, DefaultExceptionHandler {

    private static final Logger log = LogManager.getLogger(DefaultExceptionHandler.class);

    private static final int HARDWARE_PIN_MODE_MSG_ID = 1;

    private final Holder holder;
    private final DBManager dbManager;
    private final BlockingIOProcessor blockingIOProcessor;
    private final String listenPort;

    public HardwareLoginHandler(Holder holder, int listenPort) {
        this.holder = holder;
        this.dbManager = holder.dbManager;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.listenPort = String.valueOf(listenPort);
    }

    private static void completeLogin(Channel channel, Session session, User user,
                                      DashBoard dash, Device device, int msgId) {
        log.debug("completeLogin. {}", channel);

        session.addHardChannel(channel);
        channel.write(ok(msgId));

        String body = dash.buildPMMessage(device.id);
        if (dash.isActive && body.length() > 2) {
            channel.write(makeASCIIStringMessage(HARDWARE, HARDWARE_PIN_MODE_MSG_ID, body));
        }

        channel.flush();

        session.sendToApps(HARDWARE_CONNECTED, msgId, dash.id, device.id);
        log.trace("Connected device id {}, dash id {}", device.id, dash.id);
        device.connected();
        device.lastLoggedIP = IPUtils.getIp(channel.remoteAddress());

        log.info("{} hardware joined.", user.email);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
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

        ctx.pipeline().remove(this);
        ctx.pipeline().remove(HardwareNotLoggedHandler.class);
        HardwareStateHolder hardwareStateHolder = new HardwareStateHolder(user, tokenValue.dash, device);
        ctx.pipeline().addLast("HHArdwareHandler", new HardwareHandler(holder, hardwareStateHolder));

        Session session = holder.sessionDao.getOrCreateSessionByUser(
                hardwareStateHolder.userKey, ctx.channel().eventLoop());

        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering hard channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture ->
                    completeLogin(channelFuture.channel(), session, user, dash, device, message.id));
        } else {
            completeLogin(ctx.channel(), session, user, dash, device, message.id);
        }
    }

    private void checkTokenOnOtherServer(ChannelHandlerContext ctx, String token, int msgId) {
        blockingIOProcessor.executeDB(() -> {
            //check cache first
            LRUCache.CacheEntry cacheEntry = LRUCache.LOGIN_TOKENS_CACHE.get(token);

            String server;
            if (cacheEntry == null) {
                log.debug("Checking invalid token in DB.");
                server = dbManager.getServerByToken(token);
                LRUCache.LOGIN_TOKENS_CACHE.put(token, new LRUCache.CacheEntry(server));
                if (server != null) {
                    log.info("Redirecting token '{}' to {}", token, server);
                }
            } else {
                log.debug("Taking invalid token from cache.");
                server = cacheEntry.value;
            }

            // no server found, that's means token is wrong.
            if (server == null || server.equals(holder.host)) {
                log.debug("HardwareLogic token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
                ctx.writeAndFlush(invalidToken(msgId), ctx.voidPromise());
            } else {
                ctx.writeAndFlush(makeASCIIStringMessage(
                        CONNECT_REDIRECT, msgId, server + BODY_SEPARATOR + listenPort),
                        ctx.voidPromise());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }

}

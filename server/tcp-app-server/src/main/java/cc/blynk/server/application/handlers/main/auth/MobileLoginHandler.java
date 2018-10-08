package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.MobileHandler;
import cc.blynk.server.application.handlers.main.MobileResetPasswordHandler;
import cc.blynk.server.application.handlers.sharing.auth.MobileShareLoginHandler;
import cc.blynk.server.common.handlers.UserNotLoggedHandler;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.FacebookTokenResponse;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.internal.ReregisterChannelUtil;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.IPUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.server.core.protocol.enums.Command.OUTDATED_APP_NOTIFICATION;
import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleGeneralException;
import static cc.blynk.server.internal.CommonByteBufUtil.facebookUserLoginWithPass;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.makeASCIIStringMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.notAuthenticated;
import static cc.blynk.server.internal.CommonByteBufUtil.notRegistered;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;


/**
 * Handler responsible for managing apps login messages.
 * Initializes netty channel with a state tied with user.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class MobileLoginHandler extends SimpleChannelInboundHandler<LoginMessage> {

    private static final String URL = "https://graph.facebook.com/me?fields=email&access_token=";
    private static final Logger log = LogManager.getLogger(MobileLoginHandler.class);

    private final Holder holder;
    private final DefaultAsyncHttpClient asyncHttpClient;
    private final boolean allowStoreIp;

    public MobileLoginHandler(Holder holder) {
        this.holder = holder;
        this.asyncHttpClient = holder.asyncHttpClient;
        this.allowStoreIp = holder.props.getAllowStoreIp();
    }

    private static void cleanPipeline(DefaultChannelPipeline pipeline) {
        pipeline.removeIfExists(MobileLoginHandler.class);
        pipeline.removeIfExists(UserNotLoggedHandler.class);
        pipeline.removeIfExists(MobileGetServerHandler.class);
        pipeline.removeIfExists(MobileRegisterHandler.class);
        pipeline.removeIfExists(MobileShareLoginHandler.class);
        pipeline.removeIfExists(MobileResetPasswordHandler.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) {
        String[] messageParts = message.body.split(BODY_SEPARATOR_STRING);

        if (messageParts.length < 2) {
            log.error("Wrong income message format.");
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        ///.trim() is not used for back compatibility
        String email = messageParts[0].toLowerCase();

        Version version = messageParts.length > 3
                ? new Version(messageParts[2], messageParts[3])
                : Version.UNKNOWN_VERSION;

        if (messageParts.length == 5) {
            if (AppNameUtil.FACEBOOK.equals(messageParts[4])) {
                facebookLogin(ctx, message.id, email, messageParts[1], version);
            } else {
                String appName = messageParts[4];
                blynkLogin(ctx, message.id, email, messageParts[1], version, appName);
            }
        } else {
            //todo this is for back compatibility
            blynkLogin(ctx, message.id, email, messageParts[1], version, AppNameUtil.BLYNK);
        }
    }

    private void facebookLogin(ChannelHandlerContext ctx, int messageId, String email,
                               String token, Version version) {
        asyncHttpClient.prepareGet(URL + token)
                .execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) {
                        if (response.getStatusCode() != 200) {
                            String errMessage = response.getResponseBody();
                            if (errMessage != null && errMessage.contains("expired")) {
                                log.warn("Facebook token expired for user {}.", email);
                            } else {
                                log.warn("Error getting facebook token for user {}. Reason : {}", email, errMessage);
                            }
                            ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
                            return response;
                        }

                        try {
                            String responseBody = response.getResponseBody();
                            FacebookTokenResponse facebookTokenResponse =
                                    JsonParser.parseFacebookTokenResponse(responseBody);
                            if (email.equalsIgnoreCase(facebookTokenResponse.email)) {
                                User user = holder.userDao.getByName(email, AppNameUtil.BLYNK);
                                if (user == null) {
                                    user = holder.userDao.addFacebookUser(email, AppNameUtil.BLYNK);
                                }

                                login(ctx, messageId, user, version);
                            }
                        } catch (Exception e) {
                            log.error("Error during facebook response parsing for user {}. Reason : {}",
                                    email, response.getResponseBody());
                            ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
                        }

                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        log.error("Error performing facebook request. Token {} for user {}. Reason : {}",
                                token, email, t.getMessage());
                        ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
                    }
                });
    }

    private void blynkLogin(ChannelHandlerContext ctx, int msgId, String email, String pass,
                            Version version, String appName) {
        var user = holder.userDao.getByName(email, appName);

        if (user == null) {
            log.warn("User '{}' not registered. {}", email, ctx.channel().remoteAddress());
            ctx.writeAndFlush(notRegistered(msgId), ctx.voidPromise());
            return;
        }

        if (user.pass == null) {
            log.warn("Facebook user '{}' tries to login with pass. {}", email, ctx.channel().remoteAddress());
            ctx.writeAndFlush(facebookUserLoginWithPass(msgId), ctx.voidPromise());
            return;
        }

        if (!user.pass.equals(pass)) {
            log.warn("User '{}' credentials are wrong. {}", email, ctx.channel().remoteAddress());
            ctx.writeAndFlush(notAuthenticated(msgId), ctx.voidPromise());
            return;
        }

        login(ctx, msgId, user, version);
    }

    private void login(ChannelHandlerContext ctx, int messageId, User user, Version version) {
        var pipeline = (DefaultChannelPipeline) ctx.pipeline();
        cleanPipeline(pipeline);

        var appStateHolder = new MobileStateHolder(user, version);
        pipeline.addLast("AAppHandler", new MobileHandler(holder, appStateHolder));

        var channel = ctx.channel();

        //todo back compatibility code. remove in future.
        if (user.region == null || user.region.isEmpty()) {
            user.region = holder.props.region;
        }

        var session = holder.sessionDao.getOrCreateSessionByUser(appStateHolder.userKey, channel.eventLoop());
        if (session.isSameEventLoop(channel)) {
            completeLogin(channel, session, user, messageId, version);
        } else {
            log.debug("Re registering app channel. {}", ctx.channel());
            ReregisterChannelUtil.reRegisterChannel(ctx, session, channelFuture ->
                    completeLogin(channelFuture.channel(), session, user, messageId, version));
        }
    }

    private void completeLogin(Channel channel, Session session, User user, int msgId, Version version) {
        if (allowStoreIp) {
            user.lastLoggedIP = IPUtils.getIp(channel.remoteAddress());
        }
        user.lastLoggedAt = System.currentTimeMillis();

        session.addAppChannel(channel);
        channel.writeAndFlush(ok(msgId), channel.voidPromise());
        for (DashBoard dashBoard : user.profile.dashBoards) {
            if (dashBoard.isAppConnectedOn && dashBoard.isActive) {
                log.trace("{}-{}. Sending App Connected event to hardware for project {}.",
                        user.email, user.appName, dashBoard.id);
                session.sendMessageToHardware(dashBoard.id, BLYNK_INTERNAL, 7777, "acon");
            }
        }

        if (version.isOutdated()) {
            channel.writeAndFlush(
                    makeASCIIStringMessage(OUTDATED_APP_NOTIFICATION, msgId,
                            "Your app is outdated. Please update to the latest app version. "
                                    + "Ignoring this notice may affect your projects."),
                    channel.voidPromise());
        }

        log.info("{} {}-app ({}) joined.", user.email, user.appName, version);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleGeneralException(ctx, cause);
    }

}

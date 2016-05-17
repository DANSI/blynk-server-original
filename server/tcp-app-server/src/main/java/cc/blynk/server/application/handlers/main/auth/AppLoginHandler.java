package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.AppHandler;
import cc.blynk.server.application.handlers.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.UserNotAuthenticated;
import cc.blynk.server.core.protocol.exceptions.UserNotRegistered;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import io.netty.channel.*;

import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static cc.blynk.utils.ByteBufUtil.makeResponse;
import static cc.blynk.utils.ByteBufUtil.ok;


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
public class AppLoginHandler extends SimpleChannelInboundHandler<LoginMessage> implements DefaultExceptionHandler, DefaultReregisterHandler {

    private final Holder holder;
    private final FacebookLoginCheck facebookLoginCheck;

    public AppLoginHandler(Holder holder) {
        this.holder = holder;
        this.facebookLoginCheck = new FacebookLoginCheck();
    }

    private static void cleanPipeline(ChannelPipeline pipeline) {
        pipeline.remove(AppLoginHandler.class);
        pipeline.remove(UserNotLoggedHandler.class);
        pipeline.remove(RegisterHandler.class);
        pipeline.remove(AppShareLoginHandler.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split("\0");

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        final String username = messageParts[0].toLowerCase();
        final OsType osType = messageParts.length > 3 ? OsType.parse(messageParts[2]) : OsType.OTHER;
        final String version = messageParts.length > 3 ? messageParts[3] : null;

        if (messageParts.length == 5 && AppName.FACEBOOK.equals(messageParts[4])) {
            facebookLogin(ctx, message.id, username, messageParts[1], osType, version);
        } else {
            blynkLogin(ctx, message.id, username, messageParts[1], osType, version);
        }
    }

    private void facebookLogin(ChannelHandlerContext ctx, int messageId, String username, String token, OsType osType, String version) {
        holder.blockingIOProcessor.execute(() -> {
            try {
                facebookLoginCheck.verify(username, token);
            } catch (Exception e) {
                log.error("Error verifying facebook token {} for user {}.", token, username, e);
                ctx.writeAndFlush(makeResponse(messageId, NOT_ALLOWED), ctx.voidPromise());
                return;
            }

            User user = holder.userDao.getByName(username);
            if (user == null) {
                user = holder.userDao.addFacebookUser(username, AppName.BLYNK);
            }

            login(ctx, messageId, user, osType, version);
        });
    }

    private void blynkLogin(ChannelHandlerContext ctx, int messageId, String username, String pass, OsType osType, String version) {
        User user = holder.userDao.getByName(username);

        if (user == null) {
            throw new UserNotRegistered(String.format("User not registered. Username '%s', %s", username, ctx.channel().remoteAddress()), messageId);
        }

        if (!user.pass.equals(pass)) {
            throw new UserNotAuthenticated(String.format("User credentials are wrong. Username '%s', %s", username, ctx.channel().remoteAddress()), messageId);
        }

        login(ctx, messageId, user, osType, version);
    }

    private void login(ChannelHandlerContext ctx, int messageId, User user, OsType osType, String version) {
        AppStateHolder appStateHolder = new AppStateHolder(user, osType, version);

        cleanPipeline(ctx.pipeline());
        ctx.pipeline().addLast(new AppHandler(holder, appStateHolder));

        Session session = holder.sessionDao.getSessionByUser(user, ctx.channel().eventLoop());
        user.lastLoggedAt = System.currentTimeMillis();

        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering app channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), session, user.name, messageId));
        } else {
            completeLogin(ctx.channel(), session, user.name, messageId);
        }
    }

    private void completeLogin(Channel channel, Session session, String userName, int msgId) {
        session.addAppChannel(channel);
        channel.writeAndFlush(ok(msgId), channel.voidPromise());
        log.info("{} app joined.", userName);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}

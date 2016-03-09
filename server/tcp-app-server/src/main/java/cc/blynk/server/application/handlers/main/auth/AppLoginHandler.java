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
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import io.netty.channel.*;

import static cc.blynk.server.core.protocol.enums.Response.*;


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

    public AppLoginHandler(Holder holder) {
       this.holder = holder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
        //warn: split may be optimized
        //todo remove space after app migration
        String[] messageParts = message.body.split(" |\0");

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        String osType = null;
        String version = null;
        if (messageParts.length == 4) {
            osType = messageParts[2];
            version = messageParts[3];
        }
        appLogin(ctx, message.id, messageParts[0].toLowerCase(), messageParts[1], osType, version);
    }

    private void appLogin(ChannelHandlerContext ctx, int messageId, String username, String pass, String osType, String version) {
        User user = holder.userDao.getByName(username);

        if (user == null) {
            throw new UserNotRegistered(String.format("User not registered. Username '%s', %s", username, ctx.channel().remoteAddress()), messageId);
        }

        if (!user.pass.equals(pass)) {
            throw new UserNotAuthenticated(String.format("User credentials are wrong. Username '%s', %s", username, ctx.channel().remoteAddress()), messageId);
        }

        final AppStateHolder appStateHolder = new AppStateHolder(user, osType, version);
        //todo finish.
        //if (appStateHolder.isOldAPI()) {
        //    throw new NotSupportedVersion(messageId);
        //}

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
        channel.writeAndFlush(new ResponseMessage(msgId, OK), channel.voidPromise());
        log.info("{} app joined.", userName);
    }

    private void cleanPipeline(ChannelPipeline pipeline) {
        pipeline.remove(this);
        pipeline.remove(UserNotLoggedHandler.class);
        pipeline.remove(RegisterHandler.class);
        pipeline.remove(AppShareLoginHandler.class);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}

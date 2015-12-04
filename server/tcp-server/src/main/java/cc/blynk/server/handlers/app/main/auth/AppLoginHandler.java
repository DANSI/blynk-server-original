package cc.blynk.server.handlers.app.main.auth;

import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.model.messages.protocol.appllication.LoginMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.UserNotAuthenticated;
import cc.blynk.server.exceptions.UserNotRegistered;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.app.main.AppHandler;
import cc.blynk.server.handlers.app.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.*;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

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

    private final ServerProperties props;
    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final ReportingDao reportingDao;
    private final BlockingIOProcessor blockingIOProcessor;

    public AppLoginHandler(ServerProperties props, UserDao userDao, SessionDao sessionDao, ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor) {
        this.props = props;
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split(" ");

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        String osType = null;
        String version = null;
        if (messageParts.length == 4) {
            osType = messageParts[2];
            version = messageParts[3];
        }
        appLogin(ctx, message.id, messageParts[0], messageParts[1], osType, version);
    }

    private void appLogin(ChannelHandlerContext ctx, int messageId, String username, String pass, String osType, String version) {
        String userName = username.toLowerCase();
        User user = userDao.getByName(userName);

        if (user == null) {
            throw new UserNotRegistered(String.format("User not registered. Username '%s', %s", userName, ctx.channel().remoteAddress()), messageId);
        }

        if (!user.pass.equals(pass)) {
            throw new UserNotAuthenticated(String.format("User credentials are wrong. Username '%s', %s", userName, ctx.channel().remoteAddress()), messageId);
        }

        cleanPipeline(ctx.pipeline());
        ctx.pipeline().addLast(new AppHandler(props, userDao, sessionDao, reportingDao, blockingIOProcessor, new AppStateHolder(user, osType, version)));

        Session session = sessionDao.getSessionByUser(user, ctx.channel().eventLoop());

        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering app channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), session, user.name, messageId));
        } else {
            completeLogin(ctx.channel(), session, user.name, messageId);
        }
    }

    private void completeLogin(Channel channel, Session session, String userName, int msgId) {
        session.appChannels.add(channel);
        channel.writeAndFlush(produce(msgId, OK));
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

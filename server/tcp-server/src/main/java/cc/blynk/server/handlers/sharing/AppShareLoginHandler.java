package cc.blynk.server.handlers.sharing;

import cc.blynk.common.enums.Command;
import cc.blynk.common.enums.Response;
import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.model.messages.protocol.appllication.sharing.ShareLoginMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.app.auth.AppLoginHandler;
import cc.blynk.server.handlers.app.auth.RegisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggerHandler;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 * Handler responsible for managing apps sharing login messages.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class AppShareLoginHandler extends SimpleChannelInboundHandler<ShareLoginMessage> implements DefaultReregisterHandler {

    private static final Logger log = LogManager.getLogger(AppShareLoginHandler.class);

    private final ServerProperties props;
    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final ReportingDao reportingDao;
    private final BlockingIOProcessor blockingIOProcessor;

    public AppShareLoginHandler(ServerProperties props, UserDao userDao, SessionDao sessionDao, ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor) {
        this.props = props;
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ShareLoginMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split(" ");

        if (messageParts.length < 2) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        } else {
            String osType = null;
            String version = null;
            if (messageParts.length == 4) {
                osType = messageParts[2];
                version = messageParts[3];
            }
            appLogin(ctx, message.id, messageParts[0], messageParts[1], osType, version);
        }
    }

    private void appLogin(ChannelHandlerContext ctx, int messageId, String username, String token, String osType, String version) {
        String userName = username.toLowerCase();

        User user = userDao.sharedTokenManager.getUserByToken(token);

        if (user == null || !user.name.equals(userName)) {
            log.debug("Share token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(new ResponseMessage(messageId, Command.RESPONSE, Response.NOT_ALLOWED));
            return;
        }

        Integer dashId = UserDao.getDashIdByToken(user.dashShareTokens, token, messageId);

        cleanPipeline(ctx.pipeline());
        ctx.pipeline().addLast(new AppShareHandler(props, userDao, sessionDao, reportingDao, blockingIOProcessor, new AppShareStateHolder(user, osType, version, token, dashId)));

        Session session = sessionDao.getSessionByUser(user, ctx.channel().eventLoop());

        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering app channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(ctx, session, user.name, messageId));
        } else {
            completeLogin(ctx, session, user.name, messageId);
        }
    }

    private void completeLogin(ChannelHandlerContext ctx, Session session, String userName, int msgId) {
        session.appChannels.add(ctx.channel());
        ctx.writeAndFlush(produce(msgId, OK));
        log.info("Shared {} app joined.", userName);
    }

    private void cleanPipeline(ChannelPipeline pipeline) {
        pipeline.remove(this);
        pipeline.remove(UserNotLoggerHandler.class);
        pipeline.remove(RegisterHandler.class);
        pipeline.remove(AppLoginHandler.class);
    }

}

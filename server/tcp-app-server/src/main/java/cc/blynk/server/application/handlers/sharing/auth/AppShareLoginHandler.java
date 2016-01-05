package cc.blynk.server.application.handlers.sharing.auth;

import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.AppShareHandler;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.ShareLoginMessage;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.utils.ServerProperties;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;

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
            String uid = null;
            if (messageParts.length > 3) {
                osType = messageParts[2];
                version = messageParts[3];
            }
            if (messageParts.length == 5) {
              uid = messageParts[4];
            }
            appLogin(ctx, message.id, messageParts[0], messageParts[1], osType, version, uid);
        }
    }

    private void appLogin(ChannelHandlerContext ctx, int messageId, String username, String token, String osType, String version, String uid) {
        String userName = username.toLowerCase();

        User user = userDao.sharedTokenManager.getUserByToken(token);

        if (user == null || !user.name.equals(userName)) {
            log.debug("Share token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(new ResponseMessage(messageId, Response.NOT_ALLOWED));
            return;
        }

        Integer dashId = UserDao.getDashIdByToken(user.dashShareTokens, token, messageId);

        cleanPipeline(ctx.pipeline());
        ctx.pipeline().addLast(new AppShareHandler(props, sessionDao, reportingDao, blockingIOProcessor, new AppShareStateHolder(user, osType, version, token, dashId)));

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
        channel.writeAndFlush(new ResponseMessage(msgId, OK));
        log.info("Shared {} app joined.", userName);
    }

    private void cleanPipeline(ChannelPipeline pipeline) {
        pipeline.remove(this);
        pipeline.remove(UserNotLoggedHandler.class);
        pipeline.remove(RegisterHandler.class);
        pipeline.remove(AppLoginHandler.class);
    }

}

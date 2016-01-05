package cc.blynk.server.handlers.hardware.auth;

import cc.blynk.common.enums.Command;
import cc.blynk.common.enums.Response;
import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.model.messages.protocol.appllication.LoginMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

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
public class HardwareLoginHandler extends SimpleChannelInboundHandler<LoginMessage> implements DefaultExceptionHandler, DefaultReregisterHandler {

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final ServerProperties props;
    private final ReportingDao reportingDao;
    private final BlockingIOProcessor blockingIOProcessor;

    public HardwareLoginHandler(ServerProperties props, UserDao userDao, SessionDao sessionDao, ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor) {
        this.props = props;
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    private static void completeLogin(Channel channel, Session session, User user, Integer dashId, int msgId) {
        log.debug("completeLogin. {}", channel);
        session.hardwareChannels.add(channel);
        channel.writeAndFlush(produce(msgId, OK));
        sendPinMode(channel, user, dashId, msgId);
        log.info("{} hardware joined.", user.name);
    }

    //send Pin Mode command in case channel connected to active dashboard with Pin Mode command that
    //was sent previously
    private static void sendPinMode(Channel channel, User user, Integer dashId, int msgId) {
        DashBoard dash = user.profile.getDashById(dashId, msgId);
        if (dash.isActive && dash.pinModeMessage != null) {
            channel.writeAndFlush(dash.pinModeMessage);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
        String token = message.body.trim();
        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.debug("HardwareLogic token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(new ResponseMessage(message.id, Command.RESPONSE, Response.INVALID_TOKEN));
            return;
        }

        final Integer dashId = UserDao.getDashIdByToken(user.dashTokens, token, message.id);

        ctx.pipeline().remove(this);
        ctx.pipeline().remove(UserNotLoggedHandler.class);
        ctx.pipeline().addLast(new HardwareHandler(props, sessionDao, reportingDao, blockingIOProcessor, new HardwareStateHolder(dashId, user, token)));

        Session session = sessionDao.getSessionByUser(user, ctx.channel().eventLoop());

        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering hard channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), session, user, dashId, message.id));
        } else {
            completeLogin(ctx.channel(), session, user, dashId, message.id);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       handleGeneralException(ctx, cause);
    }
}

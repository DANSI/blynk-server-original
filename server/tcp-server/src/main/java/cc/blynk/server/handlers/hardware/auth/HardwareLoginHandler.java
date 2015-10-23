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
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggerHandler;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split(" ", 2);

        if (messageParts.length != 1) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        String token = messageParts[0].trim();
        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.debug("HardwareLogic token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(new ResponseMessage(message.id, Command.RESPONSE, Response.INVALID_TOKEN));
            return;
        }

        final Integer dashId = UserDao.getDashIdByToken(user.dashTokens, token, message.id);

        ctx.pipeline().remove(this);
        ctx.pipeline().remove(UserNotLoggerHandler.class);
        ctx.pipeline().addLast(new HardwareHandler(props, sessionDao, reportingDao, blockingIOProcessor, new HandlerState(dashId, user, token)));

        Session session = sessionDao.getSessionByUser(user, ctx.channel().eventLoop());

        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering hard channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(ctx, session, user, dashId, message.id));
        } else {
            completeLogin(ctx, session, user, dashId, message.id);
        }
    }

    private void completeLogin(ChannelHandlerContext ctx, Session session, User user, Integer dashId, int msgId) {
        log.debug("completeLogin. {}", ctx.channel());
        session.hardwareChannels.add(ctx.channel());
        ctx.writeAndFlush(produce(msgId, OK));
        sendPinMode(ctx, user, dashId, msgId);
        log.info("{} hardware joined.", user.name);
    }

    //send Pin Mode command in case channel connected to active dashboard with Pin Mode command that
    //was sent previously
    private void sendPinMode(ChannelHandlerContext ctx, User user, Integer dashId, int msgId) {
        DashBoard dash = user.profile.getDashboardById(dashId, msgId);
        if (dash.isActive && dash.pinModeMessage != null) {
            ctx.writeAndFlush(dash.pinModeMessage);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       handleGeneralException(ctx, cause);
    }
}

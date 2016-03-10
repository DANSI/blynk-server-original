package cc.blynk.server.hardware.handlers.hardware.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.server.hardware.handlers.hardware.HardwareHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.server.core.protocol.enums.Response.*;

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

    private final Holder holder;

    public HardwareLoginHandler(Holder holder) {
        this.holder = holder;
    }

    private static void completeLogin(Channel channel, Session session, User user, DashBoard dash, int msgId) {
        log.debug("completeLogin. {}", channel);

        if (dash.pinModeMessage == null) {
            dash.pinModeMessage = new HardwareMessage(1, dash.buildPMMessage());
        }

        session.addHardChannel(channel);
        channel.write(new ResponseMessage(msgId, OK));

        if (dash.isActive && dash.pinModeMessage.length > 2) {
            channel.write(dash.pinModeMessage);
        }

        channel.flush();

        session.sendToApps(HARDWARE_CONNECTED, msgId, String.valueOf(dash.id));

        log.info("{} hardware joined.", user.name);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
        String token = message.body.trim();
        User user = holder.userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.debug("HardwareLogic token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.INVALID_TOKEN), ctx.voidPromise());
            return;
        }

        final Integer dashId = UserDao.getDashIdByToken(user.dashTokens, token, message.id);

        DashBoard dash = user.profile.getDashById(dashId);
        if (dash == null) {
            log.error("User : {} requested token {} for non-existing {} dash id.", user.name, token, dashId);
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.INVALID_TOKEN), ctx.voidPromise());
            return;
        }

        ctx.pipeline().remove(this);
        ctx.pipeline().remove(UserNotLoggedHandler.class);
        ctx.pipeline().addLast(new HardwareHandler(holder, new HardwareStateHolder(dashId, user, token)));

        Session session = holder.sessionDao.getSessionByUser(user, ctx.channel().eventLoop());

        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering hard channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), session, user, dash, message.id));
        } else {
            completeLogin(ctx.channel(), session, user, dash, message.id);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       handleGeneralException(ctx, cause);
    }
}

package cc.blynk.server.application.handlers.sharing.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.OsType;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.AppShareHandler;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.ShareLoginMessage;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.ByteBufUtil.*;

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

    private final Holder holder;

    public AppShareLoginHandler(Holder holder) {
        this.holder = holder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ShareLoginMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split("\0");

        if (messageParts.length < 2) {
            log.error("Wrong income message format.");
            ctx.writeAndFlush(makeResponse(message.id, ILLEGAL_COMMAND), ctx.voidPromise());
        } else {
            OsType osType = null;
            String version = null;
            String uid = null;
            if (messageParts.length > 3) {
                osType = OsType.parse(messageParts[2]);
                version = messageParts[3];
            }
            if (messageParts.length == 5) {
              uid = messageParts[4];
            }
            appLogin(ctx, message.id, messageParts[0], messageParts[1], osType, version, uid);
        }
    }

    private void appLogin(ChannelHandlerContext ctx, int messageId, String username, String token, OsType osType, String version, String uid) {
        String userName = username.toLowerCase();

        User user = holder.userDao.sharedTokenManager.getUserByToken(token);

        if (user == null || !user.name.equals(userName)) {
            log.debug("Share token is invalid. User : {}, token {}, {}", userName, token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(makeResponse(messageId, NOT_ALLOWED), ctx.voidPromise());
            return;
        }

        Integer dashId = UserDao.getDashIdByToken(user.dashShareTokens, token, messageId);

        DashBoard dash = user.profile.getDashById(dashId);
        if (!dash.isShared) {
            log.debug("Dashboard is not shared. User : {}, token {}, {}", userName, token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(makeResponse(messageId, NOT_ALLOWED), ctx.voidPromise());
            return;
        }

        cleanPipeline(ctx.pipeline());
        ctx.pipeline().addLast(new AppShareHandler(holder, new AppShareStateHolder(user, osType, version, token, dashId)));

        Session session = holder.sessionDao.getSessionByUser(user, ctx.channel().eventLoop());

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
        log.info("Shared {} app joined.", userName);
    }

    private void cleanPipeline(ChannelPipeline pipeline) {
        pipeline.remove(this);
        pipeline.remove(UserNotLoggedHandler.class);
        pipeline.remove(RegisterHandler.class);
        pipeline.remove(AppLoginHandler.class);
    }

}

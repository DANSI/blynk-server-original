package cc.blynk.server.application.handlers.sharing.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileGetServerHandler;
import cc.blynk.server.application.handlers.main.auth.MobileLoginHandler;
import cc.blynk.server.application.handlers.main.auth.MobileRegisterHandler;
import cc.blynk.server.application.handlers.main.auth.Version;
import cc.blynk.server.application.handlers.sharing.MobileShareHandler;
import cc.blynk.server.common.handlers.UserNotLoggedHandler;
import cc.blynk.server.core.dao.SharedTokenValue;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.ShareLoginMessage;
import cc.blynk.server.internal.ReregisterChannelUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * Handler responsible for managing apps sharing login messages.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class MobileShareLoginHandler extends SimpleChannelInboundHandler<ShareLoginMessage> {

    private static final Logger log = LogManager.getLogger(MobileShareLoginHandler.class);

    private final Holder holder;

    public MobileShareLoginHandler(Holder holder) {
        this.holder = holder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ShareLoginMessage message) {
        String[] messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (messageParts.length < 2) {
            log.error("Wrong income message format.");
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
        } else {
            //var uid = messageParts.length == 5 ? messageParts[4] : null;
            var version = messageParts.length > 3
                    ? new Version(messageParts[2], messageParts[3])
                    : Version.UNKNOWN_VERSION;
            appLogin(ctx, message.id, messageParts[0], messageParts[1], version);
        }
    }

    private void appLogin(ChannelHandlerContext ctx, int messageId, String email,
                          String token, Version version) {
        ///.trim() is not used for back compatibility
        String userName = email.toLowerCase();

        SharedTokenValue tokenValue = holder.tokenManager.getUserBySharedToken(token);

        if (tokenValue == null || !tokenValue.user.email.equals(userName)) {
            log.debug("Share token is invalid. User : {}, token {}, {}",
                    userName, token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
            return;
        }

        User user = tokenValue.user;
        int dashId = tokenValue.dashId;

        DashBoard dash = user.profile.getDashById(dashId);
        if (!dash.isShared) {
            log.debug("Dashboard is not shared. User : {}, token {}, {}",
                    userName, token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(notAllowed(messageId), ctx.voidPromise());
            return;
        }

        cleanPipeline(ctx.pipeline());
        MobileShareStateHolder mobileShareStateHolder = new MobileShareStateHolder(user, version, token, dashId);
        ctx.pipeline().addLast("AAppSHareHandler", new MobileShareHandler(holder, mobileShareStateHolder));

        Session session = holder.sessionDao.getOrCreateSessionByUser(
                mobileShareStateHolder.userKey, ctx.channel().eventLoop());

        if (session.isSameEventLoop(ctx)) {
            completeLogin(ctx.channel(), session, user.email, messageId);
        } else {
            log.debug("Re registering app channel. {}", ctx.channel());
            ReregisterChannelUtil.reRegisterChannel(ctx, session, channelFuture ->
                    completeLogin(channelFuture.channel(), session, user.email, messageId));
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
        pipeline.remove(MobileRegisterHandler.class);
        pipeline.remove(MobileLoginHandler.class);
        pipeline.remove(MobileGetServerHandler.class);
    }

}

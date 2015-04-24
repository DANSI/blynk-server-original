package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.protocol.appllication.DeActivateDashboardMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class DeActivateDashboardHandler extends BaseSimpleChannelInboundHandler<DeActivateDashboardMessage> {

    public DeActivateDashboardHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        super(props, userRegistry, sessionsHolder);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, DeActivateDashboardMessage message) {
        user.getProfile().setActiveDashId(null);

        ctx.writeAndFlush(produce(message.id, OK));
    }

}

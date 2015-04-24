package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.protocol.appllication.LoadProfileMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class LoadProfileHandler extends BaseSimpleChannelInboundHandler<LoadProfileMessage> {

    public LoadProfileHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        super(props, userRegistry, sessionsHolder);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, LoadProfileMessage message) {
        String body = user.getProfile().toString();
        ctx.writeAndFlush(produce(message.id, message.command, body));
    }

}

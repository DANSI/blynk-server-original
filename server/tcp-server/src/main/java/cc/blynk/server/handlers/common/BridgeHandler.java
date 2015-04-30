package cc.blynk.server.handlers.common;

import cc.blynk.common.model.messages.protocol.BridgeMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

import java.util.List;

@ChannelHandler.Sharable
public class BridgeHandler extends BaseSimpleChannelInboundHandler<BridgeMessage> {

    public BridgeHandler(ServerProperties props, UserRegistry userRegistry,
                         SessionsHolder sessionsHolder) {
        super(props, userRegistry, sessionsHolder);
    }

    private static boolean isInit(String body) {
        return body != null && body.length() > 0 && body.charAt(0) == 'i';
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, BridgeMessage message) {
        Session session = sessionsHolder.userSession.get(user);
        if (isInit(message.body)) {
            final String token = message.body.split("\0")[1];
            ctx.channel().attr(ChannelState.TOKEN).set(token);
            ctx.channel().write(produce(message.id, OK));
        } else {
            String token = ctx.channel().attr(ChannelState.TOKEN).get();
            if (token == null) {
                throw new NotAllowedException("Bridge token is null", message.id);
            }
            List<Channel> channels = session.getChannelsByToken(token);
            session.sendMessageToChannels(channels, message);
        }
    }
}


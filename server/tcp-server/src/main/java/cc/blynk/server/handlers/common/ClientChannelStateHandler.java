package cc.blynk.server.handlers.common;

import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.DEVICE_WENT_OFFLINE;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/20/2015.
 *
 * Removes channel from session in case it became inactive (closed from client side).
 */
@ChannelHandler.Sharable
public class ClientChannelStateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(ClientChannelStateHandler.class);

    private final SessionsHolder sessionsHolder;

    public ClientChannelStateHandler(SessionsHolder sessionsHolder) {
        this.sessionsHolder = sessionsHolder;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionsHolder.removeFromSession(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            log.trace("Channel was inactive for a long period. Closing...");
            User user = ctx.channel().attr(ChannelState.USER).get();
            if (user != null) {
                Session session = sessionsHolder.userSession.get(user);
                if (session.appChannels.size() > 0) {
                    session.sendMessageToApp(produce(0, DEVICE_WENT_OFFLINE));
                }
            }
            //channel is already closed here by ReadTimeoutHandler
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }


}

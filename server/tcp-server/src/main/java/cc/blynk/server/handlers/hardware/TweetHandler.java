package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.hardware.TweetMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.ServerBusyException;
import cc.blynk.server.exceptions.TweetBodyInvalidException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.hardware.notifications.NotificationBase;
import cc.blynk.server.handlers.hardware.notifications.TweetNotification;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.Queue;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class TweetHandler extends BaseSimpleChannelInboundHandler<TweetMessage> {

    private final Queue<NotificationBase> notificationsQueue;

    public TweetHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                        Queue<NotificationBase> notificationsQueue) {
        super(props, userRegistry, sessionsHolder);
        this.notificationsQueue = notificationsQueue;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, TweetMessage message) {
        //todo add tweet widget check
        if (message.body == null || message.body.equals("") || message.body.length() > 140) {
            throw new TweetBodyInvalidException(message.id);
        }

        try {
            notificationsQueue.add(new TweetNotification(user.getUserProfile().getTwitter(), message.body, message.id));
        } catch (IllegalStateException e) {
            throw new ServerBusyException(message.id);
        }

        //todo send response immediately?
        ctx.channel().writeAndFlush(produce(message.id, OK));
    }


}

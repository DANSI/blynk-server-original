package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.hardware.TweetMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.TweetBodyInvalidException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Twitter;
import cc.blynk.server.notifications.twitter.exceptions.TweetNotAuthorizedException;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
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
public class TweetHandler extends BaseSimpleChannelInboundHandler<TweetMessage> {

    private final NotificationsProcessor notificationsProcessor;

    public TweetHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                        NotificationsProcessor notificationsProcessor) {
        super(props, userRegistry, sessionsHolder);
        this.notificationsProcessor = notificationsProcessor;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, TweetMessage message) {
        //todo add tweet widget check
        if (message.body == null || message.body.equals("") || message.body.length() > 140) {
            throw new TweetBodyInvalidException(message.id);
        }

        Twitter twitterWidget = user.getProfile().getActiveDashboardTwitterWidget();

        if (twitterWidget == null ||
                twitterWidget.token == null || twitterWidget.token.equals("") ||
                twitterWidget.secret == null || twitterWidget.secret.equals("")) {
            throw new TweetNotAuthorizedException("User has no access token provided.", message.id);
        }

        log.trace("Sending Twit for user {}, with message : '{}'.", user.getName(), message.body);
        notificationsProcessor.twit(twitterWidget.token, twitterWidget.secret, message.body, message.id);

        //todo send response immediately?
        checkIfNotificationQuotaLimitIsNotReached(user, message);
        ctx.writeAndFlush(produce(message.id, OK));
    }


}

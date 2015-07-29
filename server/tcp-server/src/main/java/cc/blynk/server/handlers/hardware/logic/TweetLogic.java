package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.server.exceptions.NotificationBodyInvalidException;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Twitter;
import cc.blynk.server.notifications.twitter.exceptions.TwitterNotAuthorizedException;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class TweetLogic extends NotificationBase {

    private static final Logger log = LogManager.getLogger(TweetLogic.class);

    private static final int MAX_TWITTER_BODY_SIZE = 140;
    private final NotificationsProcessor notificationsProcessor;

    public TweetLogic(NotificationsProcessor notificationsProcessor, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.notificationsProcessor = notificationsProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        if (message.body == null || message.body.equals("") || message.body.length() > MAX_TWITTER_BODY_SIZE) {
            throw new NotificationBodyInvalidException(message.id);
        }

        Twitter twitterWidget = user.getProfile().getActiveDashboardWidgetByType(Twitter.class);

        if (twitterWidget == null ||
                twitterWidget.token == null || twitterWidget.token.equals("") ||
                twitterWidget.secret == null || twitterWidget.secret.equals("")) {
            throw new TwitterNotAuthorizedException("User has no access token provided.", message.id);
        }

        user.lastTweetSentTs = checkIfNotificationQuotaLimitIsNotReached(user.lastTweetSentTs, message.id);

        log.trace("Sending Twit for user {}, with message : '{}'.", user.getName(), message.body);
        notificationsProcessor.twit(ctx.channel(), twitterWidget.token, twitterWidget.secret, message.body, message.id);
    }

}

package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.exceptions.NotificationBodyInvalidException;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.notifications.twitter.exceptions.NotifNotAuthorizedException;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Sends tweets from hardware.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class TweetLogic extends NotificationBase {

    private static final Logger log = LogManager.getLogger(TweetLogic.class);

    private static final int MAX_TWITTER_BODY_SIZE = 140;
    private final BlockingIOProcessor blockingIOProcessor;

    public TweetLogic(BlockingIOProcessor blockingIOProcessor, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.blockingIOProcessor = blockingIOProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        if (message.body == null || message.body.equals("") || message.body.length() > MAX_TWITTER_BODY_SIZE) {
            throw new NotificationBodyInvalidException(message.id);
        }

        DashBoard dash = state.user.profile.getDashById(state.dashId, message.id);
        Twitter twitterWidget = dash.getWidgetByType(Twitter.class);

        if (twitterWidget == null || !dash.isActive ||
                twitterWidget.token == null || twitterWidget.token.equals("") ||
                twitterWidget.secret == null || twitterWidget.secret.equals("")) {
            throw new NotifNotAuthorizedException("User has no access token provided.", message.id);
        }

        checkIfNotificationQuotaLimitIsNotReached(message.id);

        log.trace("Sending Twit for user {}, with message : '{}'.", state.user.name, message.body);
        blockingIOProcessor.twit(ctx.channel(), twitterWidget.token, twitterWidget.secret, message.body, message.id);
    }

}

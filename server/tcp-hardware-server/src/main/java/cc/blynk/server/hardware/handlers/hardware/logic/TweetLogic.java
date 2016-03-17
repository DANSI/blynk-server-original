package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.NotificationBodyInvalidException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.hardware.exceptions.NotifNotAuthorizedException;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;

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
    private final TwitterWrapper twitterWrapper;

    public TweetLogic(BlockingIOProcessor blockingIOProcessor, TwitterWrapper twitterWrapper, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.blockingIOProcessor = blockingIOProcessor;
        this.twitterWrapper = twitterWrapper;
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
        twit(ctx.channel(), state.user.name, twitterWidget.token, twitterWidget.secret, message.body, message.id);
    }

    private void twit(Channel channel, String username, String token, String secret, String body, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                twitterWrapper.send(token, secret, body);
                channel.writeAndFlush(new ResponseMessage(msgId, OK), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending twit for user {}. Reason : {}",  username, e.getMessage());
                channel.writeAndFlush(new ResponseMessage(msgId, Response.NOTIFICATION_EXCEPTION), channel.voidPromise());
            }
        });
    }

}

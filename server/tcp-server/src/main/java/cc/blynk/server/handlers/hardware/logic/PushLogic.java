package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.server.exceptions.NotificationBodyInvalidException;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.widgets.others.Notification;
import cc.blynk.server.notifications.twitter.exceptions.TwitterNotAuthorizedException;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handler sends push notifications to Applications. Initiation is on hardware side.
 * Sends both to iOS and Android via Google Cloud Messaging service.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class PushLogic extends NotificationBase {

    private static final Logger log = LogManager.getLogger(PushLogic.class);

    private static final int MAX_PUSH_BODY_SIZE = 255;
    private final NotificationsProcessor notificationsProcessor;

    public PushLogic(NotificationsProcessor notificationsProcessor, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.notificationsProcessor = notificationsProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, HandlerState state, Message message) {
        if (message.body == null || message.body.equals("") || message.body.length() > MAX_PUSH_BODY_SIZE) {
            throw new NotificationBodyInvalidException(message.id);
        }

        DashBoard dash = state.user.profile.getDashById(state.dashId, message.id);
        Notification widget = dash.getWidgetByType(Notification.class);

        if (widget == null ||
                ((widget.token == null || widget.token.equals("")) &&
                 (widget.iOSToken == null || widget.iOSToken.equals("")))) {
            throw new TwitterNotAuthorizedException("User has no access token provided.", message.id);
        }

        checkIfNotificationQuotaLimitIsNotReached(message.id);

        log.trace("Sending push for user {}, with message : '{}'.", state.user.name, message.body);
        notificationsProcessor.push(ctx.channel(), widget, message.body, message.id);
    }


}

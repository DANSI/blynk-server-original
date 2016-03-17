package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.NotificationBodyInvalidException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.hardware.exceptions.NotifNotAuthorizedException;
import cc.blynk.server.notifications.push.GCMMessage;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.ios.IOSGCMMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.ByteBufUtil.*;

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

    private final BlockingIOProcessor blockingIOProcessor;
    private final GCMWrapper gcmWrapper;

    public PushLogic(BlockingIOProcessor blockingIOProcessor, GCMWrapper gcmWrapper, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.blockingIOProcessor = blockingIOProcessor;
        this.gcmWrapper = gcmWrapper;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        if (Notification.isWrongBody(message.body)) {
            throw new NotificationBodyInvalidException(message.id);
        }

        DashBoard dash = state.user.profile.getDashById(state.dashId, message.id);

        if (!dash.isActive) {
            log.debug("No active dashboard.");
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.NO_ACTIVE_DASHBOARD), ctx.voidPromise());
            return;
        }

        Notification widget = dash.getWidgetByType(Notification.class);

        if (widget == null || widget.hasNoToken()) {
            throw new NotifNotAuthorizedException("User has no access token provided.", message.id);
        }

        checkIfNotificationQuotaLimitIsNotReached(message.id);

        log.trace("Sending push for user {}, with message : '{}'.", state.user.name, message.body);
        push(ctx.channel(), state.user.name, widget, message.body, state.dashId, message.id);
    }

    private void push(Channel channel, String username, Notification widget, String body, int dashId, int msgId) {
        if (widget.androidTokens.size() != 0) {
            for (Map.Entry<String, String> entry : widget.androidTokens.entrySet()) {
                push(channel, username, new AndroidGCMMessage(entry.getValue(), widget.priority, body, dashId), msgId, widget.androidTokens, entry.getKey());
            }
        }

        if (widget.iOSTokens.size() != 0) {
            for (Map.Entry<String, String> entry : widget.iOSTokens.entrySet()) {
                push(channel, username, new IOSGCMMessage(entry.getValue(), widget.priority, body, dashId), msgId, widget.iOSTokens, entry.getKey());
            }
        }
    }

    private void push(Channel channel, String username, GCMMessage message, int msgId, Map<String, String> tokens, String uid) {
        blockingIOProcessor.execute(() -> {
            try {
                gcmWrapper.send(message);
                channel.writeAndFlush(ok(channel, msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending push notification from hardware. For user {}. {}",  username, e.getMessage());
                channel.writeAndFlush(makeResponse(channel, msgId, NOTIFICATION_EXCEPTION), channel.voidPromise());

                if (e.getMessage() != null && e.getMessage().contains("NotRegistered")) {
                    log.error("Removing invalid token. UID {}", uid);
                    tokens.remove(uid);
                }
            }
        });
    }

}

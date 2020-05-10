package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.processors.NotificationBase;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.utils.properties.Placeholders;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.noActiveDash;
import static cc.blynk.server.internal.CommonByteBufUtil.notificationInvalidBody;
import static cc.blynk.server.internal.CommonByteBufUtil.notificationNotAuthorized;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

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

    private final GCMWrapper gcmWrapper;

    public PushLogic(GCMWrapper gcmWrapper, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.gcmWrapper = gcmWrapper;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        if (Notification.isWrongBody(message.body)) {
            log.debug("Notification message is empty or larger than limit.");
            ctx.writeAndFlush(notificationInvalidBody(message.id), ctx.voidPromise());
            return;
        }

        DashBoard dash = state.dash;

        if (!dash.isActive) {
            log.debug("No active dashboard.");
            ctx.writeAndFlush(noActiveDash(message.id), ctx.voidPromise());
            return;
        }

        Notification widget = dash.getNotificationWidget();

        if (widget == null) {
            log.debug("User has no notifications widget.");
            ctx.writeAndFlush(notificationNotAuthorized(message.id), ctx.voidPromise());
            return;
        }

        if (widget.hasNoToken()) {
            log.debug("User has no access token provided for push widget.");
            ctx.writeAndFlush(notificationNotAuthorized(message.id), ctx.voidPromise());
            return;
        }

        long now = System.currentTimeMillis();
        checkIfNotificationQuotaLimitIsNotReached(now);

        String deviceName = state.device.name == null ? "" : state.device.name;
        String updatedBody = message.body.replace(Placeholders.DEVICE_NAME, deviceName);

        if (Notification.isWrongBody(updatedBody)) {
            log.debug("Notification message is larger than limit.");
            ctx.writeAndFlush(notificationInvalidBody(message.id), ctx.voidPromise());
            return;
        }

        log.trace("Sending push for user {}, with message : '{}'.", state.user.email, message.body);
        widget.push(gcmWrapper, updatedBody, state.dash.id);
        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}

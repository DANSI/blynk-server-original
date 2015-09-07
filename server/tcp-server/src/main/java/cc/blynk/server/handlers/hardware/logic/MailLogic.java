package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Mail;
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
public class MailLogic extends NotificationBase {

    private static final Logger log = LogManager.getLogger(MailLogic.class);

    private final NotificationsProcessor notificationsProcessor;

    public MailLogic(NotificationsProcessor notificationsProcessor, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.notificationsProcessor = notificationsProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        Mail mail = user.getProfile().getActiveDashboardWidgetByType(Mail.class);

        if (mail == null) {
            throw new NotAllowedException("User has no mail widget or active dashboard.", message.id);
        }

        if (message.body.equals("")) {
            throw new IllegalCommandException("Invalid mail notification body.", message.id);
        }

        String[] bodyParts = message.body.split("\0");

        if (bodyParts.length != 3) {
            throw new IllegalCommandException("Invalid mail notification body.", message.id);
        }

        String to = bodyParts[0];
        String subj = bodyParts[1];
        String body = bodyParts[2];

        checkIfNotificationQuotaLimitIsNotReached(message.id);

        log.trace("Sending Mail for user {}, with message : '{}'.", user.getName(), message.body);
        notificationsProcessor.mail(ctx.channel(), to, subj, body, message.id);
    }

}

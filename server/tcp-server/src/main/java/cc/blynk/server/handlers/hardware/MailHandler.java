package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.hardware.MailMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Mail;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class MailHandler extends BaseSimpleChannelInboundHandler<MailMessage> {

    private final NotificationsProcessor notificationsProcessor;

    public MailHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                       NotificationsProcessor notificationsProcessor) {
        super(props, userRegistry, sessionsHolder);
        this.notificationsProcessor = notificationsProcessor;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, MailMessage message) {
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

        user.lastMailSentTs = checkIfNotificationQuotaLimitIsNotReached(user.lastMailSentTs, message.id);

        log.trace("Sending Mail for user {}, with message : '{}'.", user.getName(), message.body);
        notificationsProcessor.mail(ctx.channel(), to, subj, body, message.id);
    }

}

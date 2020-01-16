package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.processors.NotificationBase;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.properties.Placeholders;
import cc.blynk.utils.validators.BlynkEmailValidator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.notificationError;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * Sends email from received from hardware. Via google smtp server.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class MailLogic extends NotificationBase {

    private static final Logger log = LogManager.getLogger(MailLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final MailWrapper mailWrapper;
    private final String vendorEmail;

    public MailLogic(Holder holder) {
        super(holder.limits.notificationPeriodLimitSec);
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.mailWrapper = holder.mailWrapper;
        String tmp = holder.props.vendorEmail;
        this.vendorEmail = tmp == null ? "" : tmp;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        User user = state.user;
        DashBoard dash = state.dash;

        Mail mail = dash.getMailWidget();

        if (mail == null) {
            throw new NotAllowedException("User has no mail widget.", message.id);
        }

        if (!dash.isActive) {
            throw new NotAllowedException("User has no active dashboard.", message.id);
        }

        if (message.body.isEmpty()) {
            throw new IllegalCommandException("Invalid mail notification body.");
        }

        user.checkDailyEmailLimit();

        String[] bodyParts = message.body.split("\0");

        if (bodyParts.length < 2) {
            throw new IllegalCommandException("Invalid mail notification body.");
        }

        String to;
        String subj;
        String body;

        if (bodyParts.length == 3) {
            //if widget has no TO field
            if (mail.to == null || mail.to.isEmpty()) {
                to = bodyParts[0]
                        .replace(Placeholders.VENDOR_EMAIL, vendorEmail)
                        .replace(Placeholders.DEVICE_OWNER_EMAIL, user.email);
            } else {
                //if widget has to field it has priority over hardware field
                to = mail.to;
            }
            subj = bodyParts[1];
            body = bodyParts[2];
        } else {
            to = (mail.to == null || mail.to.isEmpty()) ? user.email : mail.to;
            subj = bodyParts[0];
            body = bodyParts[1];
        }

        checkIfNotificationQuotaLimitIsNotReached();

        //minimal validation for receiver.
        if (BlynkEmailValidator.isNotValidEmail(to)) {
            throw new IllegalCommandException("Invalid mail receiver.");
        }

        String deviceName = state.device.name == null ? "" : state.device.name;

        String updatedSubj = subj.replace(Placeholders.DEVICE_NAME, deviceName)
                                 .replace(Placeholders.VENDOR_EMAIL, vendorEmail)
                                 .replace(Placeholders.DEVICE_OWNER_EMAIL, user.email);

        String updatedBody = body.replace(Placeholders.DEVICE_NAME, deviceName)
                                 .replace(Placeholders.VENDOR_EMAIL, vendorEmail)
                                 .replace(Placeholders.DEVICE_OWNER_EMAIL, user.email);

        log.trace("Sending Mail for user {}, with message : '{}'.", user.email, updatedBody);
        mail(ctx.channel(), user.email, to, updatedSubj, updatedBody, message.id, mail.isText());
        user.emailMessages++;
    }

    private void mail(Channel channel, String email, String to, String subj, String body, int msgId, boolean isText) {
        blockingIOProcessor.execute(() -> {
            try {
                if (isText) {
                    mailWrapper.sendText(to, subj, body);
                } else {
                    mailWrapper.sendHtml(to, subj, body);
                }
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from hardware. From user {}, to : {}. Reason : {}",
                        email, to, e.getMessage());
                if (channel.isActive() && channel.isWritable()) {
                    channel.writeAndFlush(notificationError(msgId), channel.voidPromise());
                }
            }
        });
    }
}

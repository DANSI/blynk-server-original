package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.processors.NotificationBase;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.notifications.mail.MailWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_ERROR;
import static cc.blynk.utils.ByteBufUtil.makeResponse;
import static cc.blynk.utils.ByteBufUtil.ok;

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

    public MailLogic(BlockingIOProcessor blockingIOProcessor, MailWrapper mailWrapper, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.blockingIOProcessor = blockingIOProcessor;
        this.mailWrapper = mailWrapper;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        DashBoard dash = state.user.profile.getDashByIdOrThrow(state.dashId);

        Mail mail = dash.getWidgetByType(Mail.class);

        if (mail == null || !dash.isActive) {
            throw new NotAllowedException("User has no mail widget or active dashboard.");
        }

        if (message.body.equals("")) {
            throw new IllegalCommandException("Invalid mail notification body.");
        }

        String[] bodyParts = message.body.split("\0");

        if (bodyParts.length < 2) {
            throw new IllegalCommandException("Invalid mail notification body.");
        }

        String to;
        String subj;
        String body;

        if (bodyParts.length == 3) {
            to = bodyParts[0];
            subj = bodyParts[1];
            body = bodyParts[2];
        } else {
            if (mail.to == null || mail.to.equals("")) {
                to = state.user.name;
            } else {
                to = mail.to;
            }
            subj = bodyParts[0];
            body = bodyParts[1];
        }

        checkIfNotificationQuotaLimitIsNotReached();

        log.trace("Sending Mail for user {}, with message : '{}'.", state.user.name, message.body);
        mail(ctx.channel(), state.user.name, to, subj, body, message.id);
    }

    private void mail(Channel channel, String username, String to, String subj, String body, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                mailWrapper.sendText(to, subj, body);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from hardware. From user {}, to : {}. Reason : {}",  username, to, e.getMessage());
                channel.writeAndFlush(makeResponse(msgId, NOTIFICATION_ERROR), channel.voidPromise());
            }
        });
    }

}

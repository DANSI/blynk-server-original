package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public MailLogic(BlockingIOProcessor blockingIOProcessor, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.blockingIOProcessor = blockingIOProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        DashBoard dash = state.user.profile.getDashById(state.dashId, message.id);

        Mail mail = dash.getWidgetByType(Mail.class);

        if (mail == null || !dash.isActive) {
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

        log.trace("Sending Mail for user {}, with message : '{}'.", state.user.name, message.body);
        blockingIOProcessor.mail(ctx.channel(), to, subj, body, message.id);
    }

}

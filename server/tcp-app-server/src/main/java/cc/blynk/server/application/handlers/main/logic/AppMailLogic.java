package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.publishing.Publishing;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.mail.QrHolder;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_ERROR;
import static cc.blynk.utils.BlynkByteBufUtil.makeResponse;
import static cc.blynk.utils.BlynkByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;

/**
 * Sends email from application.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AppMailLogic {

    private static final Logger log = LogManager.getLogger(AppMailLogic.class);
    private final String BODY;

    private final BlockingIOProcessor blockingIOProcessor;
    private final MailWrapper mailWrapper;

    public AppMailLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.BODY = blockingIOProcessor.tokenBody;
        this.mailWrapper =  holder.mailWrapper;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = StringUtils.split2(message.body);

        int dashId = ParseUtil.parseInt(split[0]);
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        //dashId Token
        if (split.length == 2) {
            int deviceId = ParseUtil.parseInt(split[1]);
            Device device = dash.getDeviceById(deviceId);

            if (device == null || device.token == null) {
                throw new IllegalCommandBodyException("Wrong device id.");
            }

            makeSingleTokenEmail(ctx, dash, device, user.name, message.id);

        //dashId theme provisionType
        } if (split.length == 3) {
            if (dash.devices.length == 0) {
                throw new IllegalCommandBodyException("No devices in project.");
            }
            Theme theme = Theme.valueOf(split[1]);
            ProvisionType provisionType = ProvisionType.valueOf(split[2]);
            dash.publishing = new Publishing(theme, provisionType);
            makePublishPreviewEmail(ctx, dash, user.name, message.id);

        //dashId
        } else {
            if (dash.devices.length == 1) {
                makeSingleTokenEmail(ctx, dash, dash.devices[0], user.name, message.id);
            } else {
                sendMultiTokenEmail(ctx, dash, user.name, message.id);
            }
        }
    }

    private void makePublishPreviewEmail(ChannelHandlerContext ctx, DashBoard dash, String to, int msgId) {
        String body;
        if (dash.publishing.provisionType == ProvisionType.DYNAMIC) {
            body = "Hello.\nYou selected Dynamic provisioning. In order to start it - please scan QR from attachment.\n";
        } else {
            if (dash.devices.length == 1) {
               body = "Hello.\nYou selected Static provisioning. In order to start it - please scan QR from attachment.\n";
            } else {
                body = "Hello.\nYou selected Static provisioning. In order to start it - please scan QR from attachment for every device in your project.\n";
            }
        }

        mail(ctx.channel(), to, "Instruction for Blynk App Preview.", body + BODY, dash, msgId);
    }

    private void makeSingleTokenEmail(ChannelHandlerContext ctx, DashBoard dash, Device device, String to, int msgId) {
        String dashName = dash.name == null ? "New Project" : dash.name;
        String deviceName = device.name == null ? "New Device" : device.name;
        String subj = "Auth Token for " + dashName + " project and device " + deviceName;
        String body = "Auth Token : " + device.token + "\n";

        log.trace("Sending single token mail for user {}, with token : '{}'.", to, device.token);
        mail(ctx.channel(), to, to, subj, body + BODY, msgId);
    }

    private void sendMultiTokenEmail(ChannelHandlerContext ctx, DashBoard dash, String to, int msgId) {
        String dashName = dash.name == null ? "New Project" : dash.name;
        String subj = "Auth Tokens for " + dashName + " project and " + dash.devices.length + " devices";

        StringBuilder body = new StringBuilder();
        for (Device device : dash.devices) {
            String deviceName = device.name == null ? "New Device" : device.name;
            body.append("Auth Token for device '")
                .append(deviceName)
                .append("' : ")
                .append(device.token)
                .append("\n");
        }

        body.append(BODY);

        log.trace("Sending multi tokens mail for user {}, with {} tokens.", to, dash.devices.length);
        mail(ctx.channel(), to, to, subj, body.toString(), msgId);
    }

    private void mail(Channel channel, String to, String subj, String body, DashBoard dash, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                QrHolder[] qrHolders = makeQRs(to, dash.devices, dash.id);
                mailWrapper.sendHtmlWithAttachment(to, subj, body, qrHolders);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from application. For user {}. Reason : {}",  to, e.getMessage());
                channel.writeAndFlush(makeResponse(msgId, NOTIFICATION_ERROR), channel.voidPromise());
            }
        });
    }

    private static QrHolder[] makeQRs(String to, Device[] devices, int dashId) {
        QrHolder[] qrHolders = new QrHolder[devices.length];
        int i = 0;
        for (Device device : devices) {
            final String name = device.token + "_" + dashId + "_" + device.id + ".jpg";
            final String qrCode = to + BODY_SEPARATOR_STRING + dashId + BODY_SEPARATOR_STRING + device.token;
            qrHolders[i++] = new QrHolder(name, QRCode.from(qrCode).to(ImageType.JPG).stream().toByteArray());
        }
        return qrHolders;
    }

    private void mail(Channel channel, String username, String to, String subj, String body, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                mailWrapper.sendText(to, subj, body);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from application. From user {}, to : {}. Reason : {}",  username, to, e.getMessage());
                channel.writeAndFlush(makeResponse(msgId, NOTIFICATION_ERROR), channel.voidPromise());
            }
        });
    }
}

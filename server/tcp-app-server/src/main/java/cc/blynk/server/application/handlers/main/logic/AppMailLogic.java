package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.Limits;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.publishing.Publishing;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.mail.QrHolder;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.TokenGeneratorUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_ERROR;
import static cc.blynk.utils.BlynkByteBufUtil.makeResponse;
import static cc.blynk.utils.BlynkByteBufUtil.ok;

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
    private final String TOKEN_MAIL_BODY;
    private final Limits limits;

    private final BlockingIOProcessor blockingIOProcessor;
    private final MailWrapper mailWrapper;
    private final DBManager dbManager;

    public AppMailLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.TOKEN_MAIL_BODY = holder.limits.TOKEN_BODY;
        this.mailWrapper =  holder.mailWrapper;
        this.dbManager = holder.dbManager;
        this.limits = holder.limits;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        int dashId = ParseUtil.parseInt(split[0]);
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        //dashId Token
        if (split.length == 2) {
            int deviceId = ParseUtil.parseInt(split[1]);
            Device device = dash.getDeviceById(deviceId);

            if (device == null || device.token == null) {
                throw new IllegalCommandBodyException("Wrong device id.");
            }

            makeSingleTokenEmail(ctx, dash, device, user.email, message.id);

        //dashId theme provisionType color appname
        } else if (split.length == 5) {
            if (dash.devices.length == 0) {
                throw new IllegalCommandBodyException("No devices in project.");
            }
            Theme theme = Theme.valueOf(split[1]);
            ProvisionType provisionType = ProvisionType.valueOf(split[2]);
            int color = Integer.parseInt(split[3]);
            String name = split[4];
            dash.publishing = new Publishing(theme, provisionType, color, name);
            log.debug("Sending app preview email to {}, provision type {}", user.email, provisionType);
            makePublishPreviewEmail(ctx, dash, user.email, name, user.appName, message.id);

        //dashId
        } else {
            if (dash.devices.length == 1) {
                makeSingleTokenEmail(ctx, dash, dash.devices[0], user.email, message.id);
            } else {
                sendMultiTokenEmail(ctx, dash, user.email, message.id);
            }
        }
    }

    private void makePublishPreviewEmail(ChannelHandlerContext ctx, DashBoard dash, String to, String appName, String appId, int msgId) {
        String subj = appName + " - App details";
        if (dash.publishing.provisionType == ProvisionType.DYNAMIC) {
            mailDynamic(ctx.channel(), to, subj, limits.DYNAMIC_MAIL_BODY, msgId);
        } else {
            mailStatic(ctx.channel(), to, appId, subj, limits.STATIC_MAIL_BODY, dash, msgId);
        }
    }

    private void mailDynamic(Channel channel, String to, String subj, String body, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                mailWrapper.sendHtml(to, subj, body);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from application. For user {}. Reason : {}", to, e.getMessage());
                channel.writeAndFlush(makeResponse(msgId, NOTIFICATION_ERROR), channel.voidPromise());
            }
        });
    }

    private void mailStatic(Channel channel, String to, String appName, String subj, String body, DashBoard dash, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                QrHolder[] qrHolders = makeQRs(to, appName, dash, dash.id);
                StringBuilder sb = new StringBuilder();
                for (QrHolder qrHolder : qrHolders) {
                    sb.append("<br>");
                    sb.append(qrHolder.mailBodyPart);
                }
                mailWrapper.sendWithAttachment(to, subj, body.replace("{device_section}", sb.toString()), qrHolders);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from application. For user {}. Reason : {}", to, e.getMessage());
                channel.writeAndFlush(makeResponse(msgId, NOTIFICATION_ERROR), channel.voidPromise());
            }
        });
    }

    private void makeSingleTokenEmail(ChannelHandlerContext ctx, DashBoard dash, Device device, String to, int msgId) {
        String dashName = dash.name == null ? "New Project" : dash.name;
        String deviceName = device.name == null ? "New Device" : device.name;
        String subj = "Auth Token for " + dashName + " project and device " + deviceName;
        String body = "Auth Token : " + device.token + "\n";

        log.trace("Sending single token mail for user {}, with token : '{}'.", to, device.token);
        mail(ctx.channel(), to, to, subj, body + TOKEN_MAIL_BODY, msgId);
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

        body.append(TOKEN_MAIL_BODY);

        log.trace("Sending multi tokens mail for user {}, with {} tokens.", to, dash.devices.length);
        mail(ctx.channel(), to, to, subj, body.toString(), msgId);
    }

    private QrHolder[] makeQRs(String publisherEmail, String appName, DashBoard dash, int dashId) throws Exception {
        QrHolder[] qrHolders = new QrHolder[dash.devices.length];
        FlashedToken[] flashedTokens = new FlashedToken[dash.devices.length];

        int i = 0;
        for (Device device : dash.devices) {
            String newToken = TokenGeneratorUtil.generateNewToken();
            String name = newToken + "_" + dashId + "_" + device.id + ".jpg";
            String qrCode = newToken + " " + dashId + " " + publisherEmail;
            String mailBodyPart = device.name + ": " + newToken;
            qrHolders[i] = new QrHolder(name, mailBodyPart, QRCode.from(qrCode).to(ImageType.JPG).stream().toByteArray());
            flashedTokens[i] = new FlashedToken(newToken, appName, device.id);
            i++;
        }

        if (!dbManager.insertFlashedTokens(flashedTokens)) {
            throw new Exception("App Publishing Preview requires enabled DB.");
        }

        return qrHolders;
    }

    private void mail(Channel channel, String email, String to, String subj, String body, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                mailWrapper.sendText(to, subj, body);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from application. From user {}, to : {}. Reason : {}",  email, to, e.getMessage());
                channel.writeAndFlush(makeResponse(msgId, NOTIFICATION_ERROR), channel.voidPromise());
            }
        });
    }
}

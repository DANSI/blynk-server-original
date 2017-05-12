package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.Limits;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.ProvisionType;
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
public class MailQRsLogic {

    private static final Logger log = LogManager.getLogger(MailQRsLogic.class);
    private final Limits limits;

    private final BlockingIOProcessor blockingIOProcessor;
    private final MailWrapper mailWrapper;
    private final DBManager dbManager;

    public MailQRsLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.mailWrapper =  holder.mailWrapper;
        this.dbManager = holder.dbManager;
        this.limits = holder.limits;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        int dashId = ParseUtil.parseInt(split[0]);
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        if (dash.devices.length == 0) {
            throw new IllegalCommandBodyException("No devices in project.");
        }

        String appId = split[1];
        App app = user.profile.getAppById(appId);

        if (app == null) {
            throw new IllegalCommandBodyException("App with passed id not found.");
        }

        log.debug("Sending app preview email to {}, provision type {}", user.email, app.provisionType);
        makePublishPreviewEmail(ctx, dash, app.provisionType, user.email, app.name, appId, message.id);
    }

    private void makePublishPreviewEmail(ChannelHandlerContext ctx, DashBoard dash, ProvisionType provisionType, String to, String publishAppName, String publishAppId, int msgId) {
        String subj = publishAppName + " - App details";
        if (provisionType == ProvisionType.DYNAMIC) {
            mailDynamic(ctx.channel(), to, subj, limits.DYNAMIC_MAIL_BODY, publishAppId, dash, msgId);
        } else {
            mailStatic(ctx.channel(), to, subj, limits.STATIC_MAIL_BODY, publishAppId, dash, msgId);
        }
    }

    private void mailDynamic(Channel channel, String to, String subj, String body, String publishAppId, DashBoard dash, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                QrHolder[] qrHolders = makeQRs(publishAppId, dash, true);

                mailWrapper.sendWithAttachment(to, subj, body, qrHolders);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from application. For user {}. Reason : {}", to, e.getMessage());
                channel.writeAndFlush(makeResponse(msgId, NOTIFICATION_ERROR), channel.voidPromise());
            }
        });
    }

    private void mailStatic(Channel channel, String to, String subj, String body, String publishAppId, DashBoard dash, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                QrHolder[] qrHolders = makeQRs(publishAppId, dash, false);
                StringBuilder sb = new StringBuilder();
                for (QrHolder qrHolder : qrHolders) {
                    qrHolder.attach(sb);
                }
                mailWrapper.sendWithAttachment(to, subj, body.replace("{device_section}", sb.toString()), qrHolders);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error sending email from application. For user {}. Reason : {}", to, e.getMessage());
                channel.writeAndFlush(makeResponse(msgId, NOTIFICATION_ERROR), channel.voidPromise());
            }
        });
    }

    private QrHolder[] makeQRs(String appName, DashBoard dash, boolean onlyFirst) throws Exception {
        QrHolder[] qrHolders = new QrHolder[dash.devices.length];
        FlashedToken[] flashedTokens = new FlashedToken[dash.devices.length];

        int i = 0;
        for (Device device : dash.devices) {
            if (onlyFirst && i > 0) {
                break;
            }
            String newToken = TokenGeneratorUtil.generateNewToken();
            qrHolders[i] = new QrHolder(dash.id, device.id, device.name, newToken, QRCode.from(newToken).to(ImageType.JPG).stream().toByteArray());
            flashedTokens[i] = new FlashedToken(newToken, appName, dash.id, device.id);
            i++;
        }

        if (!dbManager.insertFlashedTokens(flashedTokens)) {
            throw new Exception("App Publishing Preview requires enabled DB.");
        }

        return qrHolders;
    }
}

package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Device;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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

        String dashBoardIdString = split[0];
        String deviceIdString = "0";

        if (split.length == 2) {
            deviceIdString = split[1];
        }

        int dashId = ParseUtil.parseInt(dashBoardIdString);
        int deviceId = ParseUtil.parseInt(deviceIdString);

        DashBoard dashBoard = user.profile.getDashByIdOrThrow(dashId);
        Device device = dashBoard.getDeviceById(deviceId);

        String token = device.token;

        if (token == null) {
            throw new IllegalCommandBodyException("Wrong device id.");
        }

        String to = user.name;
        String name = dashBoard.name == null ? "New Project" : dashBoard.name;
        String subj = "Auth Token for " + name + " project";
        String body = String.format(BODY, name, token);

        log.trace("Sending Mail for user {}, with token : '{}'.", user.name, token);
        mail(ctx.channel(), user.name, to, subj, body, message.id);
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

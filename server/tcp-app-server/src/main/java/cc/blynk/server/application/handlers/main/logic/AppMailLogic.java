package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;

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
    private final static String SUBJECT = "Auth Token for %s project";
    private final String BODY;

    private final BlockingIOProcessor blockingIOProcessor;
    private final MailWrapper mailWrapper;

    public AppMailLogic(BlockingIOProcessor blockingIOProcessor, MailWrapper mailWrapper) {
        this.blockingIOProcessor = blockingIOProcessor;
        this.BODY = blockingIOProcessor.tokenBody;
        this.mailWrapper = mailWrapper;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId = ParseUtil.parseInt(dashBoardIdString, message.id);

        DashBoard dashBoard = user.profile.getDashById(dashId, message.id);
        String token = user.dashTokens.get(dashId);

        if (token == null) {
            throw new IllegalCommandBodyException("Wrong dash id.", message.id);
        }

        String to = user.name;
        String name = dashBoard.name == null ? "New Project" : dashBoard.name;
        String subj = String.format(SUBJECT, name);
        String body = String.format(BODY, name, token);

        log.trace("Sending Mail for user {}, with token : '{}'.", user.name, token);
        mail(ctx.channel(), user.name, to, subj, body, message.id);
    }

    private void mail(Channel channel, String username, String to, String subj, String body, int msgId) {
        blockingIOProcessor.execute(() -> {
            try {
                mailWrapper.send(to, subj, body);
                channel.writeAndFlush(new ResponseMessage(msgId, OK));
            } catch (Exception e) {
                log.error("Error sending email from application. For user {}.",  username, e);
                channel.writeAndFlush(new ResponseMessage(msgId, Response.NOTIFICATION_EXCEPTION));
            }
        });
    }
}

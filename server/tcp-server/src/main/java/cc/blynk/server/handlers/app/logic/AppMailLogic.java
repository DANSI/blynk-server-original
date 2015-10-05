package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.server.exceptions.IllegalCommandBodyException;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private final static String BODY = "Auth Token for %s project \"%s\"\n" +
            "\n" +
            "Happy Blynking!\n" +
            "-\n" +
            "http://www.blynk.cc\n" +
            "twitter.com/blynk_app\n" +
            "www.facebook.com/blynkapp";
    private final NotificationsProcessor notificationsProcessor;

    public AppMailLogic(NotificationsProcessor notificationsProcessor) {
        this.notificationsProcessor = notificationsProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        String dashBoardIdString = message.body;

        int dashId;
        try {
            dashId = Integer.parseInt(dashBoardIdString);
        } catch (NumberFormatException ex) {
            throw new IllegalCommandBodyException(String.format("Dash board id '%s' not valid.", dashBoardIdString), message.id);
        }

        DashBoard dashBoard = user.profile.getDashById(dashId);
        String token = user.dashTokens.get(dashId);

        if (dashBoard == null || token == null) {
            throw new IllegalCommandBodyException("Wrong dash id.", message.id);
        }

        String to = user.name;
        String name = dashBoard.name == null ? "New Project" : dashBoard.name;
        String subj = String.format(SUBJECT, name);
        String body = String.format(BODY, name, token);

        log.trace("Sending Mail for user {}, with message : '{}'.", user.name, message.body);
        notificationsProcessor.mail(ctx.channel(), to, subj, body, message.id);
    }

}

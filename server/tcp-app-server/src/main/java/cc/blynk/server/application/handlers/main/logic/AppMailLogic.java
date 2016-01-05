package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
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
    private final String BODY;

    private final BlockingIOProcessor blockingIOProcessor;

    public AppMailLogic(BlockingIOProcessor blockingIOProcessor) {
        this.blockingIOProcessor = blockingIOProcessor;
        this.BODY = blockingIOProcessor.tokenBody;
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
        blockingIOProcessor.mail(ctx.channel(), to, subj, body, message.id);
    }

}

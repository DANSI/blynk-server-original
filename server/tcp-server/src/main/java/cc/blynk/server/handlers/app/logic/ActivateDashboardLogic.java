package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class ActivateDashboardLogic {

    private static final Logger log = LogManager.getLogger(ActivateDashboardLogic.class);

    public static void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        String dashBoardIdString = message.body;

        int dashBoardId;
        try {
            dashBoardId = Integer.parseInt(dashBoardIdString);
        } catch (NumberFormatException ex) {
            throw new IllegalCommandException(String.format("Dash board id '%s' not valid.", dashBoardIdString), message.id);
        }

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.name);
        user.profile.validateDashId(dashBoardId, message.id);
        user.profile.activeDashId = dashBoardId;

        ctx.writeAndFlush(produce(message.id, OK));
    }

}

package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.model.DashBoard;
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

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId = ParseUtil.parseInt(dashBoardIdString, message.id);

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.name);
        DashBoard dashBoard = user.profile.getDashboardById(dashId, message.id);
        user.profile.activeDashId = dashId;
        dashBoard.isActive = true;

        ctx.writeAndFlush(produce(message.id, OK));
    }

}

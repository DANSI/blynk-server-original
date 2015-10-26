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
public class DeActivateDashboardLogic {

    private static final Logger log = LogManager.getLogger(ActivateDashboardLogic.class);

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        if (message.length > 0) {
            log.debug("Activating dash {} for user {}", message.body, user.name);
            int dashId = ParseUtil.parseInt(message.body, message.id);
            DashBoard dashBoard = user.profile.getDashboardById(dashId, message.id);
            dashBoard.isActive = false;
        } else {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                dashBoard.isActive = false;
            }
        }
        user.profile.activeDashId = null;
        ctx.writeAndFlush(produce(message.id, OK));
    }

}

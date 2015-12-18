package cc.blynk.server.handlers.app.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;
import static cc.blynk.server.utils.StateHolderUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class DeActivateDashboardLogic {

    private static final Logger log = LogManager.getLogger(ActivateDashboardLogic.class);

    private final SessionDao sessionDao;

    public DeActivateDashboardLogic(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        if (message.length > 0) {
            log.debug("DeActivating dash {} for user {}", message.body, user.name);
            int dashId = ParseUtil.parseInt(message.body, message.id);
            DashBoard dashBoard = user.profile.getDashById(dashId, message.id);
            dashBoard.isActive = false;
        } else {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                dashBoard.isActive = false;
            }
        }
        user.profile.activeDashId = null;

        Session session = sessionDao.userSession.get(user);
        for (Channel appChannel : session.appChannels) {
            if (appChannel != ctx.channel() && getAppState(appChannel) != null) {
                appChannel.writeAndFlush(message);
            }
        }

        ctx.writeAndFlush(produce(message.id, OK));
    }

}

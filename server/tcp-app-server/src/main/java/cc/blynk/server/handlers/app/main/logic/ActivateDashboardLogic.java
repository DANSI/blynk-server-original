package cc.blynk.server.handlers.app.main.logic;

import cc.blynk.common.enums.Response;
import cc.blynk.common.model.messages.StringMessage;
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
import static cc.blynk.common.utils.ParseUtil.*;
import static cc.blynk.server.utils.AppStateHolderUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class ActivateDashboardLogic {

    private static final Logger log = LogManager.getLogger(ActivateDashboardLogic.class);

    private final SessionDao sessionDao;

    public ActivateDashboardLogic(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId = parseInt(dashBoardIdString, message.id);

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.name);
        DashBoard dashBoard = user.profile.getDashById(dashId, message.id);
        dashBoard.isActive = true;
        user.lastModifiedTs = System.currentTimeMillis();

        Session session = sessionDao.userSession.get(user);

        for (Channel appChannel : session.appChannels) {
            if (appChannel != ctx.channel() && getAppState(appChannel) != null) {
                appChannel.writeAndFlush(message);
            }
        }

        if (session.hasHardwareOnline(dashId)) {
            ctx.writeAndFlush(produce(message.id, OK));
        } else {
            log.debug("No device in session.");
            ctx.writeAndFlush(produce(message.id, Response.DEVICE_NOT_IN_NETWORK));
        }
    }

}

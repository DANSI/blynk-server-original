package cc.blynk.server.handlers.app.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.exceptions.DeviceNotInNetworkException;
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
import static cc.blynk.server.utils.StateHolderUtil.*;

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
        DashBoard dashBoard = user.profile.getDashboardById(dashId, message.id);
        user.profile.activeDashId = dashId;
        dashBoard.isActive = true;

        Session session = sessionDao.userSession.get(user);

        for (Channel appChannel : session.appChannels) {
            if (getAppState(appChannel) != null) {
                appChannel.writeAndFlush(message);
            }
        }

        if (session.hasHardwareOnline(dashId)) {
            ctx.writeAndFlush(produce(message.id, OK));
        } else {
            throw new DeviceNotInNetworkException(message.id);
        }
    }

}

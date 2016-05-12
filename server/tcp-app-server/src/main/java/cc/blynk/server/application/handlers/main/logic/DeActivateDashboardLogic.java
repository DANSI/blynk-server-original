package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.AppStateHolderUtil.*;
import static cc.blynk.utils.ByteBufUtil.*;

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
            dashBoard.deactivate();
        } else {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                dashBoard.deactivate();
            }
        }
        user.lastModifiedTs = System.currentTimeMillis();

        Session session = sessionDao.userSession.get(user);
        for (Channel appChannel : session.getAppChannels()) {
            if (appChannel != ctx.channel() && getAppState(appChannel) != null) {
                appChannel.writeAndFlush(message, appChannel.voidPromise());
            }
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}

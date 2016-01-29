package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.SyncMessage;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.AppStateHolderUtil.*;

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

        int dashId = ParseUtil.parseInt(dashBoardIdString, message.id);

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.name);
        DashBoard dash = user.profile.getDashById(dashId, message.id);
        dash.activate();
        user.lastModifiedTs = System.currentTimeMillis();

        Session session = sessionDao.userSession.get(user);

        if (session.hasHardwareOnline(dashId)) {
            ctx.writeAndFlush(new ResponseMessage(message.id, OK));
        } else {
            log.debug("No device in session.");
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.DEVICE_NOT_IN_NETWORK));
        }

        List<SyncMessage> syncMessages = new ArrayList<>();
        for (Widget widget : dash.widgets) {
            String body = widget.makeHardwareBody();
            if (body != null) {
                syncMessages.add(new SyncMessage(1111, dashId + StringUtils.BODY_SEPARATOR_STRING + body));
            }
        }

        for (Channel appChannel : session.appChannels) {
            if (appChannel != ctx.channel() && getAppState(appChannel) != null) {
                appChannel.write(message);
            }
            for (SyncMessage syncMessage : syncMessages) {
                appChannel.write(syncMessage);
            }
            appChannel.flush();
        }
    }

}

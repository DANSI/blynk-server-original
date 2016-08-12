package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static cc.blynk.utils.AppStateHolderUtil.getAppState;
import static cc.blynk.utils.ByteBufUtil.makeResponse;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;
import static cc.blynk.utils.ByteBufUtil.ok;

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

        int dashId = ParseUtil.parseInt(dashBoardIdString);

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.name);
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        dash.activate();
        user.lastModifiedTs = System.currentTimeMillis();

        Session session = sessionDao.userSession.get(user);

        if (session.hasHardwareOnline(dashId)) {
            session.sendMessageToHardware(ctx, dashId, HARDWARE, 1, dash.buildPMMessage());

            ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
        } else {
            log.debug("No device in session.");
            ctx.writeAndFlush(makeResponse(message.id, DEVICE_NOT_IN_NETWORK), ctx.voidPromise());
        }

        for (Channel appChannel : session.getAppChannels()) {
            if (appChannel != ctx.channel() && getAppState(appChannel) != null) {
                appChannel.write(makeStringMessage(message.command, message.id, message.body));
            }

            for (Widget widget : dash.widgets) {
                widget.sendSyncOnActivate(appChannel, dashId);
            }

            appChannel.flush();
        }
    }

}

package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.model.widgets.AppSyncWidget.ANY_TARGET;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.BlynkByteBufUtil.deviceNotInNetwork;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.BlynkByteBufUtil.ok;
import static cc.blynk.utils.AppStateHolderUtil.getAppState;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class ActivateDashboardLogic {

    private static final int PIN_MODE_MSG_ID = 1;

    private static final Logger log = LogManager.getLogger(ActivateDashboardLogic.class);

    private final SessionDao sessionDao;

    public ActivateDashboardLogic(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        User user = state.user;
        String dashBoardIdString = message.body;

        int dashId = ParseUtil.parseInt(dashBoardIdString);

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.email);
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        dash.activate();
        user.lastModifiedTs = dash.updatedAt;

        Session session = sessionDao.userSession.get(state.userKey);

        if (session.isHardwareConnected(dashId)) {
            for (Device device : dash.devices) {
                if (session.sendMessageToHardware(dashId, HARDWARE, PIN_MODE_MSG_ID,
                        dash.buildPMMessage(device.id), device.id)) {
                    log.debug("No device in session.");
                    if (ctx.channel().isWritable() && !dash.isNotificationsOff) {
                        ctx.write(deviceNotInNetwork(PIN_MODE_MSG_ID), ctx.voidPromise());
                    }
                }
            }

            ctx.write(ok(message.id), ctx.voidPromise());
        } else {
            log.debug("No device in session.");
            if (!dash.isNotificationsOff) {
                ctx.write(deviceNotInNetwork(message.id), ctx.voidPromise());
            }
        }

        for (Channel appChannel : session.appChannels) {
            //send activate for shared apps
            if (appChannel != ctx.channel() && getAppState(appChannel) != null && appChannel.isWritable()) {
                appChannel.write(makeUTF8StringMessage(message.command, message.id, message.body));
            }

            dash.sendSyncs(appChannel, ANY_TARGET);
            appChannel.flush();
        }
    }

}

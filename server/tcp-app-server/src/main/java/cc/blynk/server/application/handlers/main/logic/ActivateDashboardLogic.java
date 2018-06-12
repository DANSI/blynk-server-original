package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.model.widgets.AppSyncWidget.ANY_TARGET;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.deviceNotInNetwork;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
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
        var user = state.user;
        var dashBoardIdString = message.body;

        var dashId = Integer.parseInt(dashBoardIdString);

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.email);
        var dash = user.profile.getDashByIdOrThrow(dashId);
        dash.activate();
        user.lastModifiedTs = dash.updatedAt;

        var session = sessionDao.userSession.get(state.userKey);

        if (session.isHardwareConnected(dashId)) {
            for (Device device : dash.devices) {
                var pmBody = dash.buildPMMessage(device.id);
                if (pmBody == null) {
                    if (!session.isHardwareConnected(dashId, device.id)) {
                        log.debug("No device in session.");
                        if (ctx.channel().isWritable() && !dash.isNotificationsOff) {
                            ctx.write(deviceNotInNetwork(PIN_MODE_MSG_ID), ctx.voidPromise());
                        }
                    }
                } else {
                    if (device.fitsBufferSize(pmBody.length())) {
                        if (session.sendMessageToHardware(dashId, HARDWARE, PIN_MODE_MSG_ID, pmBody, device.id)) {
                            log.debug("No device in session.");
                            if (ctx.channel().isWritable() && !dash.isNotificationsOff) {
                                ctx.write(deviceNotInNetwork(PIN_MODE_MSG_ID), ctx.voidPromise());
                            }
                        }
                    } else {
                        ctx.write(deviceNotInNetwork(message.id), ctx.voidPromise());
                        log.warn("PM message is to large for {}, size : {}", user.email, pmBody.length());
                    }
                }
            }

            ctx.write(ok(message.id), ctx.voidPromise());
        } else {
            log.debug("No device in session.");
            if (dash.isNotificationsOff) {
                ctx.write(ok(message.id), ctx.voidPromise());
            } else {
                ctx.write(deviceNotInNetwork(message.id), ctx.voidPromise());
            }
        }
        ctx.flush();

        for (var appChannel : session.appChannels) {
            //send activate for shared apps
            AppStateHolder appStateHolder = getAppState(appChannel);
            if (appChannel != ctx.channel() && appStateHolder != null && appChannel.isWritable()) {
                appChannel.write(makeUTF8StringMessage(message.command, message.id, message.body));
            }

            boolean isNewSyncFormat = appStateHolder != null && appStateHolder.isNewSyncFormat();
            dash.sendSyncs(appChannel, ANY_TARGET, isNewSyncFormat);
            appChannel.flush();
        }
    }

}

package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.model.widgets.MobileSyncWidget.ANY_TARGET;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.deviceNotInNetwork;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.MobileStateHolderUtil.getAppState;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class MobileActivateDashboardLogic {

    private static final int PIN_MODE_MSG_ID = 1;

    private static final Logger log = LogManager.getLogger(MobileActivateDashboardLogic.class);

    private MobileActivateDashboardLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        User user = state.user;
        String dashBoardIdString = message.body;

        int dashId = Integer.parseInt(dashBoardIdString);

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.email);
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        dash.activate();
        user.lastModifiedTs = dash.updatedAt;

        SessionDao sessionDao = holder.sessionDao;
        Session session = sessionDao.get(state.userKey);

        if (session.isHardwareConnected(dashId)) {
            for (Device device : dash.devices) {
                String pmBody = dash.buildPMMessage(device.id);
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

        for (Channel appChannel : session.appChannels) {
            //send activate for shared apps
            MobileStateHolder mobileStateHolder = getAppState(appChannel);
            if (appChannel != ctx.channel() && mobileStateHolder != null && appChannel.isWritable()) {
                appChannel.write(makeUTF8StringMessage(message.command, message.id, message.body));
            }

            user.profile.sendAppSyncs(dash, appChannel, ANY_TARGET);
            appChannel.flush();
        }
    }

}

package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.processors.BaseProcessorHandler;
import cc.blynk.server.core.processors.WebhookProcessor;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.deviceNotInNetwork;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommandBody;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.AppStateHolderUtil.getAppState;
import static cc.blynk.utils.StringUtils.split2;
import static cc.blynk.utils.StringUtils.split2Device;
import static cc.blynk.utils.StringUtils.split3;

/**
 * Responsible for handling incoming hardware commands from applications and forwarding it to
 * appropriate hardware.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareAppLogic extends BaseProcessorHandler {

    private static final Logger log = LogManager.getLogger(HardwareAppLogic.class);

    private final SessionDao sessionDao;

    public HardwareAppLogic(Holder holder, String email) {
        super(holder.eventorProcessor, new WebhookProcessor(holder.asyncHttpClient,
                holder.limits.webhookPeriodLimitation,
                holder.limits.webhookResponseSizeLimitBytes,
                holder.limits.webhookFailureLimit,
                holder.stats,
                email));
        this.sessionDao = holder.sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        var session = sessionDao.userSession.get(state.userKey);

        //here expecting command in format "1-200000 vw 88 1"
        var split = split2(message.body);

        //here we have "1-200000"
        var dashIdAndTargetIdString = split2Device(split[0]);
        var dashId = Integer.parseInt(dashIdAndTargetIdString[0]);

        var dash = state.user.profile.getDashByIdOrThrow(dashId);

        //if no active dashboard - do nothing. this could happen only in case of app. bug
        if (!dash.isActive) {
            return;
        }

        //deviceId or tagId or device selector widget id
        var targetId = 0;

        //new logic for multi devices
        if (dashIdAndTargetIdString.length == 2) {
            targetId = Integer.parseInt(dashIdAndTargetIdString[1]);
        }

        //sending message only if widget assigned to device or tag has assigned devices
        var target = dash.getTarget(targetId);
        if (target == null) {
            log.debug("No assigned target id for received command.");
            return;
        }

        var deviceIds = target.getDeviceIds();

        if (deviceIds.length == 0) {
            log.debug("No devices assigned to target.");
            return;
        }

        var operation = split[1].charAt(1);
        switch (operation) {
            case 'u' :
                //splitting "vu 200000 1"
                var splitBody = split3(split[1]);
                processDeviceSelectorCommand(ctx, session, dash, message, splitBody);
                break;
            case 'w' :
                splitBody = split3(split[1]);

                if (splitBody.length < 3) {
                    log.debug("Not valid write command.");
                    ctx.writeAndFlush(illegalCommandBody(message.id), ctx.voidPromise());
                    return;
                }

                var pinType = PinType.getPinType(splitBody[0].charAt(0));
                var pin = Byte.parseByte(splitBody[1]);
                var value = splitBody[2];
                var now = System.currentTimeMillis();

                for (int deviceId : deviceIds) {
                    dash.update(deviceId, pin, pinType, value, now);
                }

                //additional state for tag widget itself
                if (target.isTag()) {
                    dash.update(targetId, pin, pinType, value, now);
                }

                //sending to shared dashes and master-master apps
                session.sendToSharedApps(ctx.channel(), dash.sharedToken, APP_SYNC, message.id, message.body);

                if (session.sendMessageToHardware(dashId, HARDWARE, message.id, split[1], deviceIds)
                        && !dash.isNotificationsOff) {
                    log.debug("No device in session.");
                    ctx.writeAndFlush(deviceNotInNetwork(message.id), ctx.voidPromise());
                }

                processEventorAndWebhook(state.user, dash, targetId, session, pin, pinType, value, now);
                break;
        }
    }

    public static void processDeviceSelectorCommand(ChannelHandlerContext ctx,
                                                    Session session, DashBoard dash,
                                                    StringMessage message, String[] splitBody) {
        //in format "vu 200000 1"
        var widgetId = Long.parseLong(splitBody[1]);
        var deviceSelector = dash.getWidgetByIdOrThrow(widgetId);
        if (deviceSelector instanceof DeviceSelector) {
            var selectedDeviceId = Integer.parseInt(splitBody[2]);
            ((DeviceSelector) deviceSelector).value = selectedDeviceId;
            ctx.write(ok(message.id), ctx.voidPromise());

            //sending to shared dashes and master-master apps
            session.sendToSharedApps(ctx.channel(), dash.sharedToken, APP_SYNC, message.id, message.body);

            //we need to send syncs not only to main app, but all to all shared apps
            for (Channel channel : session.appChannels) {
                AppStateHolder appStateHolder = getAppState(channel);
                if (appStateHolder != null && appStateHolder.contains(dash.sharedToken)) {
                    boolean isNewSyncFormat = appStateHolder.isNewSyncFormat();
                    dash.sendAppSyncs(channel, selectedDeviceId, isNewSyncFormat);
                }
                channel.flush();
            }
        }
    }

}

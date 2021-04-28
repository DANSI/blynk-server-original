package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.processors.BaseProcessorHandler;
import cc.blynk.server.core.processors.WebhookProcessor;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.NumberUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.deviceNotInNetwork;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommandBody;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.MobileStateHolderUtil.getAppState;
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
public class MobileHardwareLogic extends BaseProcessorHandler {

    private static final Logger log = LogManager.getLogger(MobileHardwareLogic.class);

    private final SessionDao sessionDao;

    public MobileHardwareLogic(Holder holder, String email) {
        super(holder.eventorProcessor, new WebhookProcessor(holder.asyncHttpClient,
                holder.limits.webhookPeriodLimitation,
                holder.limits.webhookResponseSizeLimitBytes,
                holder.limits.webhookFailureLimit,
                holder.stats,
                email));
        this.sessionDao = holder.sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state, StringMessage message) {
        Session session = sessionDao.get(state.userKey);

        //here expecting command in format "1-200000 vw 88 1"
        String[] split = split2(message.body);

        //here we have "1-200000"
        String[] dashIdAndTargetIdString = split2Device(split[0]);
        int dashId = Integer.parseInt(dashIdAndTargetIdString[0]);

        Profile profile = state.user.profile;
        DashBoard dash = profile.getDashByIdOrThrow(dashId);

        //if no active dashboard - do nothing. this could happen only in case of app. bug
        if (!dash.isActive) {
            return;
        }

        //deviceId or tagId or device selector widget id
        int targetId = 0;

        //new logic for multi devices
        if (dashIdAndTargetIdString.length == 2) {
            targetId = Integer.parseInt(dashIdAndTargetIdString[1]);
        }

        //sending message only if widget assigned to device or tag has assigned devices
        Target target;
        if (targetId < Tag.START_TAG_ID) {
            target = profile.getDeviceById(dash, targetId);
        } else if (targetId < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
            target = profile.getTagById(dash, targetId);
        } else {
            //means widget assigned to device selector widget.
            target = dash.getDeviceSelector(targetId);
        }

        if (target == null) {
            log.debug("No assigned target id for received command.");
            return;
        }

        int[] deviceIds = target.getDeviceIds();

        if (deviceIds.length == 0) {
            log.debug("No devices assigned to target.");
            return;
        }

        char operation = split[1].charAt(1);
        switch (operation) {
            case 'u' :
                //splitting "vu 200000 1"
                String[] splitBody = split3(split[1]);
                processDeviceSelectorCommand(ctx, session, state.user.profile, dash, message, splitBody);
                break;
            case 'w' :
                splitBody = split3(split[1]);

                if (splitBody.length < 3) {
                    log.debug("Not valid write command.");
                    ctx.writeAndFlush(illegalCommandBody(message.id), ctx.voidPromise());
                    return;
                }

                PinType pinType = PinType.getPinType(splitBody[0].charAt(0));
                short pin = NumberUtil.parsePin(splitBody[1]);
                String value = splitBody[2];
                long now = System.currentTimeMillis();

                for (int deviceId : deviceIds) {
                    profile.update(dash, deviceId, pin, pinType, value, now);
                }

                //additional state for tag widget itself
                if (target.isTag()) {
                    profile.update(dash, targetId, pin, pinType, value, now);
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
                                                    Session session, Profile profile, DashBoard dash,
                                                    StringMessage message, String[] splitBody) {
        //in format "vu 200000 1"
        long widgetId = Long.parseLong(splitBody[1]);
        Widget deviceSelector = dash.getWidgetByIdOrThrow(widgetId);
        if (deviceSelector instanceof DeviceSelector) {
            int selectedDeviceId = Integer.parseInt(splitBody[2]);
            ((DeviceSelector) deviceSelector).value = selectedDeviceId;
            ctx.write(ok(message.id), ctx.voidPromise());

            //sending to shared dashes and master-master apps
            session.sendToSharedApps(ctx.channel(), dash.sharedToken, APP_SYNC, message.id, message.body);

            //we need to send syncs not only to main app, but all to all shared apps
            for (Channel channel : session.appChannels) {
                MobileStateHolder mobileStateHolder = getAppState(channel);
                if (mobileStateHolder != null && mobileStateHolder.contains(dash.sharedToken)) {
                    profile.sendAppSyncs(dash, channel, selectedDeviceId);
                }
                channel.flush();
            }
        }
    }

}

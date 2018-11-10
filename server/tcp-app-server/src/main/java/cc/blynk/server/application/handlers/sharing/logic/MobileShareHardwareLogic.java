package cc.blynk.server.application.handlers.sharing.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.logic.MobileHardwareLogic;
import cc.blynk.server.application.handlers.sharing.auth.MobileShareStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Target;
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
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.utils.StringUtils.split2;
import static cc.blynk.utils.StringUtils.split2Device;
import static cc.blynk.utils.StringUtils.split3;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class MobileShareHardwareLogic extends BaseProcessorHandler {

    private static final Logger log = LogManager.getLogger(MobileShareHardwareLogic.class);

    private final SessionDao sessionDao;

    public MobileShareHardwareLogic(Holder holder, String email) {
        super(holder.eventorProcessor, new WebhookProcessor(holder.asyncHttpClient,
                holder.limits.webhookPeriodLimitation,
                holder.limits.webhookResponseSizeLimitBytes,
                holder.limits.webhookFailureLimit,
                holder.stats,
                email));
        this.sessionDao = holder.sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, MobileShareStateHolder state, StringMessage message) {
        Session session = sessionDao.get(state.userKey);

        //here expecting command in format "1-200000 vw 88 1"
        String[] split = split2(message.body);

        //here we have "1-200000"
        String[] dashIdAndTargetIdString = split2Device(split[0]);
        int dashId = Integer.parseInt(dashIdAndTargetIdString[0]);

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

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

        if (!dash.isShared) {
            log.debug("Dashboard is not shared. User : {}, {}", user.email, ctx.channel().remoteAddress());
            ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
            return;
        }

        //sending message only if widget assigned to device or tag has assigned devices
        Target target;
        if (targetId < Tag.START_TAG_ID) {
            target = user.profile.getDeviceById(dash, targetId);
        } else if (targetId < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
            target = user.profile.getTagById(dash, targetId);
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
                MobileHardwareLogic.processDeviceSelectorCommand(ctx, session, user.profile, dash, message, splitBody);
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
                    user.profile.update(dash, deviceId, pin, pinType, value, now);
                }

                //additional state for tag widget itself
                if (target.isTag()) {
                    user.profile.update(dash, targetId, pin, pinType, value, now);
                }

                String sharedToken = state.token;
                if (sharedToken != null) {
                    for (Channel appChannel : session.appChannels) {
                        if (appChannel != ctx.channel() && appChannel.isWritable()
                                && Session.needSync(appChannel, sharedToken)) {
                            appChannel.writeAndFlush(
                                    makeUTF8StringMessage(APP_SYNC, message.id, message.body),
                                    appChannel.voidPromise());
                        }
                    }
                }

                if (session.sendMessageToHardware(dashId, HARDWARE, message.id, split[1], deviceIds)
                        && !dash.isNotificationsOff) {
                    log.debug("No device in session.");
                    ctx.writeAndFlush(deviceNotInNetwork(message.id), ctx.voidPromise());
                }

                processEventorAndWebhook(user, dash, targetId, session, pin, pinType, value, now);
                break;
        }
    }
}

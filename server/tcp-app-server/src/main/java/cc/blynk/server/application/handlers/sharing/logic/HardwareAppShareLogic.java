package cc.blynk.server.application.handlers.sharing.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.logic.HardwareAppLogic;
import cc.blynk.server.application.handlers.sharing.auth.AppShareStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.processors.BaseProcessorHandler;
import cc.blynk.server.core.processors.WebhookProcessor;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
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
public class HardwareAppShareLogic extends BaseProcessorHandler {

    private static final Logger log = LogManager.getLogger(HardwareAppShareLogic.class);

    private final SessionDao sessionDao;

    public HardwareAppShareLogic(Holder holder, String email) {
        super(holder.eventorProcessor, new WebhookProcessor(holder.asyncHttpClient,
                holder.limits.webhookPeriodLimitation,
                holder.limits.webhookResponseSizeLimitBytes,
                holder.limits.webhookFailureLimit,
                holder.stats,
                email));
        this.sessionDao = holder.sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppShareStateHolder state, StringMessage message) {
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

        if (!dash.isShared) {
            log.debug("Dashboard is not shared. User : {}, {}", state.user.email, ctx.channel().remoteAddress());
            ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
            return;
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
                HardwareAppLogic.processDeviceSelectorCommand(ctx, session, dash, message, splitBody);
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

                var sharedToken = state.token;
                if (sharedToken != null) {
                    for (var appChannel : session.appChannels) {
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

                processEventorAndWebhook(state.user, dash, targetId, session, pin, pinType, value, now);
                break;
        }
    }
}

package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.processors.BaseProcessorHandler;
import cc.blynk.server.core.processors.WebhookProcessor;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.utils.StringUtils.split2;
import static cc.blynk.utils.StringUtils.split2Device;
import static cc.blynk.utils.StringUtils.split3;

/**
 * Handler responsible for processing messages that are forwarded
 * by application to server from Bluetooth module.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareResendFromBTLogic extends BaseProcessorHandler {

    private final ReportingDiskDao reportingDao;
    private final SessionDao sessionDao;

    public HardwareResendFromBTLogic(Holder holder, String email) {
        super(holder.eventorProcessor, new WebhookProcessor(holder.asyncHttpClient,
                holder.limits.webhookPeriodLimitation,
                holder.limits.webhookResponseSizeLimitBytes,
                holder.limits.webhookFailureLimit,
                holder.stats,
                email));
        this.sessionDao = holder.sessionDao;
        this.reportingDao = holder.reportingDiskDao;
    }

    private static boolean isWriteOperation(String body) {
        return body.charAt(1) == 'w';
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        //minimum command - "1-1 vw 1"
        if (message.body.length() < 8) {
            log.debug("HardwareResendFromBTLogic command body too short.");
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        var split = split2(message.body);

        //here we have "1-200000"
        var dashIdAndTargetIdString = split2Device(split[0]);
        var dashId = Integer.parseInt(dashIdAndTargetIdString[0]);
        var deviceId = Integer.parseInt(dashIdAndTargetIdString[1]);

        var dash = state.user.profile.getDashByIdOrThrow(dashId);

        if (isWriteOperation(split[1])) {
            var splitBody = split3(split[1]);

            if (splitBody.length < 3 || splitBody[0].length() == 0 || splitBody[2].length() == 0) {
                log.debug("Write command is wrong.");
                ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
                return;
            }

            var pinType = PinType.getPinType(splitBody[0].charAt(0));
            var pin = Byte.parseByte(splitBody[1]);
            var value = splitBody[2];
            var now = System.currentTimeMillis();

            reportingDao.process(state.user, dash, deviceId, pin, pinType, value, now);
            dash.update(deviceId, pin, pinType, value, now);

            var session = sessionDao.userSession.get(state.userKey);
            processEventorAndWebhook(state.user, dash, deviceId, session, pin, pinType, value, now);
        }
    }

}

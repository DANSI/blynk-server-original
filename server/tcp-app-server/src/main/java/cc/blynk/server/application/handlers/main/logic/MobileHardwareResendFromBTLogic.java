package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.processors.BaseProcessorHandler;
import cc.blynk.server.core.processors.WebhookProcessor;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.NumberUtil;
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
public class MobileHardwareResendFromBTLogic extends BaseProcessorHandler {

    private final ReportingDiskDao reportingDao;
    private final SessionDao sessionDao;

    public MobileHardwareResendFromBTLogic(Holder holder, String email) {
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

    public void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state, StringMessage message) {
        //minimum command - "1-1 vw 1"
        if (message.body.length() < 8) {
            log.debug("MobileHardwareResendFromBTLogic command body too short.");
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        String[] split = split2(message.body);

        //here we have "1-200000"
        String[] dashIdAndTargetIdString = split2Device(split[0]);
        int dashId = Integer.parseInt(dashIdAndTargetIdString[0]);
        int deviceId = Integer.parseInt(dashIdAndTargetIdString[1]);

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        if (isWriteOperation(split[1])) {
            String[] splitBody = split3(split[1]);

            if (splitBody.length < 3 || splitBody[0].length() == 0 || splitBody[2].length() == 0) {
                log.debug("Write command is wrong.");
                ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
                return;
            }

            PinType pinType = PinType.getPinType(splitBody[0].charAt(0));
            short pin = NumberUtil.parsePin(splitBody[1]);
            String value = splitBody[2];
            long now = System.currentTimeMillis();

            reportingDao.process(user, dash, deviceId, pin, pinType, value, now);
            user.profile.update(dash, deviceId, pin, pinType, value, now);

            Session session = sessionDao.get(state.userKey);
            processEventorAndWebhook(user, dash, deviceId, session, pin, pinType, value, now);
        }
    }

}

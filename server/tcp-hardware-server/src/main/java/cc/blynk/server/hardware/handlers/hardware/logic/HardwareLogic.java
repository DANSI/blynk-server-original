package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.processors.BaseProcessorHandler;
import cc.blynk.server.core.processors.WebhookProcessor;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.NumberUtil;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.utils.StringUtils.split3;

/**
 * Handler responsible for forwarding messages from hardware to applications.
 * Also handler stores all incoming hardware commands to disk in order to export and
 * analyze data.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareLogic extends BaseProcessorHandler {

    private final ReportingDiskDao reportingDao;
    private final SessionDao sessionDao;

    public HardwareLogic(Holder holder, String email) {
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

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        messageReceived(ctx, message, state.userKey, state.user, state.dash, state.device);
    }

    public void messageReceived(ChannelHandlerContext ctx, StringMessage message,
                                UserKey userKey, User user, DashBoard dash, Device device) {
        String body = message.body;

        //minimum command - "ar 1"
        if (body.length() < 4) {
            log.debug("HardwareLogic command body too short.");
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        if (isWriteOperation(body)) {
            String[] splitBody = split3(body);

            if (splitBody.length < 3 || splitBody[0].length() == 0 || splitBody[2].length() == 0) {
                log.debug("Write command is wrong {} for {} and deviceId {}.", body, user.email, device.id);
                ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
                return;
            }

            PinType pinType = PinType.getPinType(splitBody[0].charAt(0));
            short pin = NumberUtil.parsePin(splitBody[1]);
            String value = splitBody[2];
            long now = System.currentTimeMillis();
            int deviceId = device.id;

            reportingDao.process(user, dash, deviceId, pin, pinType, value, now);
            user.profile.update(dash, deviceId, pin, pinType, value, now);
            device.dataReceivedAt = now;

            Session session = sessionDao.get(userKey);
            processEventorAndWebhook(user, dash, deviceId, session, pin, pinType, value, now);

            if (dash.isActive) {
                session.sendToApps(HARDWARE, message.id, dash.id, deviceId, body);
            } else {
                log.trace("No active dashboard.");
            }
        }
    }

}

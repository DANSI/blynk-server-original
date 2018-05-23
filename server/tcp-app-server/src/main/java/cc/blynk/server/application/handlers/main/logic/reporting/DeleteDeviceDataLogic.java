package cc.blynk.server.application.handlers.main.logic.reporting;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2Device;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class DeleteDeviceDataLogic {

    private static final Logger log = LogManager.getLogger(DeleteDeviceDataLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingDao reportingDao;

    public DeleteDeviceDataLogic(ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor) {
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    private static int[] getDeviceIds(Device[] devices) {
        int[] deviceIds = new int[devices.length];
        for (int i = 0; i < devices.length; i++) {
            deviceIds[i] = devices[i].id;
        }
        return deviceIds;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] messageParts = StringUtils.split2(message.body);

        if (messageParts.length < 1) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        String[] dashIdAndDeviceId = split2Device(messageParts[0]);
        int dashId = Integer.parseInt(dashIdAndDeviceId[0]);
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        if ("*".equals(dashIdAndDeviceId[1])) {
            int[] deviceIds = getDeviceIds(dash.devices);
            delete(ctx.channel(), message.id, user, dash, deviceIds);
        } else {
            int deviceId = Integer.parseInt(dashIdAndDeviceId[1]);

            //we have only deviceId
            if (messageParts.length == 1) {
                delete(ctx.channel(), message.id, user, dash, deviceId);
            } else {
                //we have deviceId and datastreams to clean
                delete(ctx.channel(), message.id, user, dash, deviceId,
                        messageParts[1].split(StringUtils.BODY_SEPARATOR_STRING));
            }
        }
    }

    private void delete(Channel channel, int msgId, User user, DashBoard dash, int... deviceIds) {
        blockingIOProcessor.executeHistory(() -> {
            try {
                for (int deviceId : deviceIds) {
                    int removedCounter = reportingDao.delete(user, dash.id, deviceId);
                    log.debug("Removed {} files for dashId {} and deviceId {}", removedCounter, dash.id, deviceId);
                }
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.debug("Error removing enhanced graph data. Reason : {}.", e.getMessage());
                channel.writeAndFlush(illegalCommand(msgId), channel.voidPromise());
            }
        });
    }

    private void delete(Channel channel, int msgId, User user, DashBoard dash, int deviceId, String[] pins) {
        blockingIOProcessor.executeHistory(() -> {
            try {
                int removedCounter = reportingDao.delete(user, dash.id, deviceId, pins);
                log.debug("Removed {} files for dashId {} and deviceId {}", removedCounter, dash.id, deviceId);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.debug("Error removing enhanced graph data. Reason : {}.", e.getMessage());
                channel.writeAndFlush(illegalCommand(msgId), channel.voidPromise());
            }
        });
    }

}

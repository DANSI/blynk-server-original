package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.application.handlers.sharing.auth.MobileShareStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
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
public final class MobileDeleteDeviceDataLogic {

    private static final Logger log = LogManager.getLogger(MobileDeleteDeviceDataLogic.class);

    private MobileDeleteDeviceDataLogic() {
    }

    private static int[] getDeviceIds(Device[] devices) {
        int[] deviceIds = new int[devices.length];
        for (int i = 0; i < devices.length; i++) {
            deviceIds[i] = devices[i].id;
        }
        return deviceIds;
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        String[] messageParts = StringUtils.split2(message.body);

        if (messageParts.length < 1) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        String[] dashIdAndDeviceId = split2Device(messageParts[0]);
        int dashId = Integer.parseInt(dashIdAndDeviceId[0]);
        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        //if app is shared - check if we can remove devices
        if (state instanceof MobileShareStateHolder) {
            ReportingWidget reportingWidget = dash.getReportingWidget();
            if (reportingWidget == null) {
                throw new IllegalCommandException("No reporting widget.");
            }
            if (!reportingWidget.allowEndUserToDeleteDataOn) {
                throw new NotAllowedException("You are not allowed to delete the data.", message.id);
            }
        }

        if ("*".equals(dashIdAndDeviceId[1])) {
            int[] deviceIds = getDeviceIds(dash.devices);
            delete(holder, ctx.channel(), message.id, user, dash, deviceIds);
        } else {
            int deviceId = Integer.parseInt(dashIdAndDeviceId[1]);

            //we have only deviceId
            if (messageParts.length == 1) {
                delete(holder, ctx.channel(), message.id, user, dash, deviceId);
            } else {
                //we have deviceId and datastreams to clean
                delete(holder,  ctx.channel(), message.id, user, dash, deviceId,
                        messageParts[1].split(StringUtils.BODY_SEPARATOR_STRING));
            }
        }
    }

    private static void delete(Holder holder, Channel channel, int msgId, User user, DashBoard dash, int... deviceIds) {
        holder.blockingIOProcessor.executeHistory(() -> {
            try {
                for (int deviceId : deviceIds) {
                    int removedCounter = holder.reportingDiskDao.delete(user, dash.id, deviceId);
                    log.debug("Removed {} files for dashId {} and deviceId {}", removedCounter, dash.id, deviceId);
                }
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.warn("Error removing device data. Reason : {}.", e.getMessage());
                channel.writeAndFlush(illegalCommand(msgId), channel.voidPromise());
            }
        });
    }

    private static void delete(Holder holder,  Channel channel, int msgId,
                        User user, DashBoard dash, int deviceId, String[] pins) {
        holder.blockingIOProcessor.executeHistory(() -> {
            try {
                int removedCounter = holder.reportingDiskDao.delete(user, dash.id, deviceId, pins);
                log.debug("Removed {} files for dashId {} and deviceId {}", removedCounter, dash.id, deviceId);
                channel.writeAndFlush(ok(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.warn("Error removing device data. Reason : {}.", e.getMessage());
                channel.writeAndFlush(illegalCommand(msgId), channel.voidPromise());
            }
        });
    }

}

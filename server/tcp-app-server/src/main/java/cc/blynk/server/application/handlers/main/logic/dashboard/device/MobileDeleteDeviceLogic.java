package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class MobileDeleteDeviceLogic {

    private static final Logger log = LogManager.getLogger(MobileDeleteDeviceLogic.class);

    private MobileDeleteDeviceLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        int deviceId = Integer.parseInt(split[1]);

        User user = state.user;
        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        log.debug("Deleting device with id {}.", deviceId);

        Device device = user.profile.deleteDevice(dash, deviceId);
        user.profile.cleanPinStorageForDevice(deviceId);
        user.profile.deleteDeviceFromTags(dash, deviceId);
        holder.tokenManager.deleteDevice(device);
        Session session = holder.sessionDao.get(state.userKey);
        session.closeHardwareChannelByDeviceId(dashId, deviceId);

        user.lastModifiedTs = System.currentTimeMillis();

        holder.blockingIOProcessor.executeHistory(() -> {
            try {
                holder.reportingDiskDao.delete(user, dashId, deviceId);
            } catch (Exception e) {
                log.warn("Error removing device data. Reason : {}.", e.getMessage());
            }
        });

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}

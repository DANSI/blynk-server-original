package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
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
public class DeleteDeviceLogic {

    private static final Logger log = LogManager.getLogger(DeleteDeviceLogic.class);

    private final TokenManager tokenManager;
    private final SessionDao sessionDao;

    public DeleteDeviceLogic(TokenManager tokenManager, SessionDao sessionDao) {
        this.tokenManager = tokenManager;
        this.sessionDao = sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        var split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashId = Integer.parseInt(split[0]);
        var deviceId = Integer.parseInt(split[1]);

        var dash = state.user.profile.getDashByIdOrThrow(dashId);

        log.debug("Deleting device with id {}.", deviceId);

        var existingDeviceIndex = dash.getDeviceIndexById(deviceId);
        var device = dash.devices[existingDeviceIndex];
        tokenManager.deleteDevice(device);
        var session = sessionDao.userSession.get(state.userKey);
        session.closeHardwareChannelByDeviceId(dashId, deviceId);

        dash.devices = ArrayUtil.remove(dash.devices, existingDeviceIndex, Device.class);
        dash.updatedAt = System.currentTimeMillis();
        state.user.lastModifiedTs = dash.updatedAt;

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}

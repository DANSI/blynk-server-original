package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.BlynkByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public class DeleteDeviceLogic {

    private static final Logger log = LogManager.getLogger(DeleteDeviceLogic.class);

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(split[0]) ;
        int deviceId = ParseUtil.parseInt(split[1]);

        if (deviceId == 0) {
            throw new IllegalCommandException("You are not allowed to remove device with id 0.");
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        log.debug("Deleting device with id {}.", deviceId);

        int existingDeviceIndex = dash.getDeviceIndexById(deviceId);

        dash.devices = ArrayUtil.remove(dash.devices, existingDeviceIndex);
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}

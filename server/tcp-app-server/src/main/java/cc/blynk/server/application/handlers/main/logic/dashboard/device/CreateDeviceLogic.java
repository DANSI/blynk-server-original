package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.TokenGeneratorUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.CREATE_DEVICE;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class CreateDeviceLogic {

    private static final Logger log = LogManager.getLogger(CreateDeviceLogic.class);

    private CreateDeviceLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       User user, StringMessage message) {
        var split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashId = Integer.parseInt(split[0]);
        var deviceString = split[1];

        if (deviceString == null || deviceString.isEmpty()) {
            throw new IllegalCommandException("Income device message is empty.");
        }

        var dash = user.profile.getDashByIdOrThrow(dashId);

        if (dash.devices.length > holder.limits.deviceLimit) {
            throw new NotAllowedException("Device limit is reached.", message.id);
        }

        var newDevice = JsonParser.parseDevice(deviceString, message.id);

        log.debug("Creating new device {}.", deviceString);

        if (newDevice.isNotValid()) {
            throw new IllegalCommandException("Income device message is not valid.");
        }

        for (var device : dash.devices) {
            if (device.id == newDevice.id) {
                throw new NotAllowedException("Device with same id already exists.", message.id);
            }
        }

        dash.devices = ArrayUtil.add(dash.devices, newDevice, Device.class);

        var newToken = TokenGeneratorUtil.generateNewToken();
        holder.tokenManager.assignToken(user, dash, newDevice, newToken);

        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(
                    makeUTF8StringMessage(CREATE_DEVICE, message.id, newDevice.toString()), ctx.voidPromise());
        }
    }

}

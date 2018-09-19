package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommandBody;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class UpdateDeviceLogic {

    private static final Logger log = LogManager.getLogger(UpdateDeviceLogic.class);

    private UpdateDeviceLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
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

        var newDevice = JsonParser.parseDevice(deviceString, message.id);

        log.debug("Updating new device {}.", deviceString);

        if (newDevice.isNotValid()) {
            throw new IllegalCommandException("Income device message is not valid.");
        }

        var existingDevice = dash.getDeviceById(newDevice.id);

        if (existingDevice == null) {
            log.debug("Attempt to update device with non existing id.");
            ctx.writeAndFlush(illegalCommandBody(message.id), ctx.voidPromise());
            return;
        }

        existingDevice.update(newDevice);
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}

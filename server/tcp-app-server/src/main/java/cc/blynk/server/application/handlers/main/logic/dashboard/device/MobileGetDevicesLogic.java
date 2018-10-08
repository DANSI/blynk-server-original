package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.DeviceStatusDTO;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.GET_DEVICES;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class MobileGetDevicesLogic {

    private MobileGetDevicesLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        int dashId = Integer.parseInt(message.body);

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        String devicesJson;
        if (dash.devices == null || dash.devices.length == 0) {
            devicesJson = "[]";
        } else {
            DeviceStatusDTO[] deviceStatusDTOS = DeviceStatusDTO.transform(dash.devices);
            devicesJson = JsonParser.toJson(deviceStatusDTOS);
        }

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(makeUTF8StringMessage(GET_DEVICES, message.id, devicesJson), ctx.voidPromise());
        }
    }

}

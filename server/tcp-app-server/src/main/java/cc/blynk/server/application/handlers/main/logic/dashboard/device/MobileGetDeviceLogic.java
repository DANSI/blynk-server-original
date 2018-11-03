package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.MOBILE_GET_DEVICE;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommandBody;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class MobileGetDeviceLogic {

    private MobileGetDeviceLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = StringUtils.split2(message.body);
        int dashId = Integer.parseInt(split[0]);
        int deviceId = Integer.parseInt(split[1]);

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        Device device = user.profile.getDeviceById(dash, deviceId);
        if (device == null) {
            ctx.writeAndFlush(illegalCommandBody(message.id), ctx.voidPromise());
        } else {
            if (ctx.channel().isWritable()) {
                ctx.writeAndFlush(makeUTF8StringMessage(MOBILE_GET_DEVICE,
                        message.id, device.toString()), ctx.voidPromise());
            }
        }
    }

}

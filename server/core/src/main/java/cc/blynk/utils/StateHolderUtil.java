package cc.blynk.utils;

import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.Channel;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class StateHolderUtil {

    public static HardwareStateHolder getHardState(Channel channel) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return handler == null ? null : (HardwareStateHolder) handler.state;
    }

    public static boolean isSameDash(Channel channel, int dashId) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return handler != null && ((HardwareStateHolder) handler.state).dashId == dashId;
    }

    public static boolean isSameDashAndDeviceId(Channel channel, int dashId, int deviceId) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);

        if (handler == null) {
            return false;
        }

        HardwareStateHolder hardwareStateHolder = (HardwareStateHolder) handler.state;

        return hardwareStateHolder.dashId == dashId && hardwareStateHolder.deviceId == deviceId;
    }

}

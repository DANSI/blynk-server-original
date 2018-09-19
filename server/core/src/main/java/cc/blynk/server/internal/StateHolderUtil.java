package cc.blynk.server.internal;

import cc.blynk.server.common.BaseSimpleChannelInboundHandler;
import cc.blynk.server.core.session.HardwareStateHolder;
import io.netty.channel.Channel;

/**
 * Used instead of Netty's DefaultAttributeMap as it faster and
 * doesn't involves any synchronization at all.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public final class StateHolderUtil {

    private StateHolderUtil() {
    }

    public static HardwareStateHolder getHardState(Channel channel) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return handler == null ? null : (HardwareStateHolder) handler.getState();
    }

    public static boolean isSameDash(Channel channel, int dashId) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return handler != null && handler.getState().isSameDash(dashId);
    }

    public static boolean isSameDashAndDeviceId(Channel channel, int dashId, int deviceId) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return handler != null && handler.getState().isSameDashAndDeviceId(dashId, deviceId);
    }

}

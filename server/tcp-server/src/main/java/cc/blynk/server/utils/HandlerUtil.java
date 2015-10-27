package cc.blynk.server.utils;

import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class HandlerUtil {

    private static final HardwareStateHolder NO_STATE = new HardwareStateHolder(null);

    public static HardwareStateHolder getState(Channel channel) {
        final ChannelHandler channelHandler = channel.pipeline().last();
        if (channelHandler instanceof BaseSimpleChannelInboundHandler) {
            return ((BaseSimpleChannelInboundHandler) channelHandler).getHandlerState();
        }
        return NO_STATE;
    }

}

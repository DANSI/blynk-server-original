package cc.blynk.server.utils;

import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import io.netty.channel.Channel;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class HandlerUtil {

    private static final HandlerState NO_STATE = new HandlerState(null, null, null);

    public static HandlerState getState(Channel channel) {
        if (!(channel.pipeline().last() instanceof BaseSimpleChannelInboundHandler)) {
            return NO_STATE;
        }
        return ((BaseSimpleChannelInboundHandler) channel.pipeline().last()).getHandlerState();
    }

}

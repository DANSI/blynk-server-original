package cc.blynk.utils;

import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class StateHolderUtil {

    public static HardwareStateHolder getHardState(Channel channel) {
        return getHardState(channel.pipeline());
    }

    private static HardwareStateHolder getHardState(ChannelPipeline pipeline) {
        BaseSimpleChannelInboundHandler handler = pipeline.get(BaseSimpleChannelInboundHandler.class);
        return handler == null ? null : (HardwareStateHolder) handler.state;
    }

}

package cc.blynk.utils;

import cc.blynk.server.core.model.auth.User;
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

    public static User getStateUser(Channel channel) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return handler == null ? null : handler.state.user;
    }
}

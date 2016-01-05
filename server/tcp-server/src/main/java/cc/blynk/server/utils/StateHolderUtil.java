package cc.blynk.server.utils;

import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.main.AppHandler;
import cc.blynk.server.handlers.app.main.auth.AppStateHolder;
import cc.blynk.server.handlers.app.sharing.AppShareHandler;
import cc.blynk.server.handlers.app.sharing.auth.AppShareStateHolder;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.model.auth.User;
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
        HardwareHandler handler = pipeline.get(HardwareHandler.class);
        return handler == null ? null : handler.state;
    }

    public static AppStateHolder getAppState(Channel channel) {
        return getAppState(channel.pipeline());
    }

    public static AppStateHolder getAppState(ChannelPipeline pipeline) {
        AppHandler handler = pipeline.get(AppHandler.class);
        if (handler == null) {
            return getShareState(pipeline);
        }
        return handler.state;
    }

    public static AppShareStateHolder getShareState(Channel channel) {
        return getShareState(channel.pipeline());
    }

    private static AppShareStateHolder getShareState(ChannelPipeline pipeline) {
        AppShareHandler handler = pipeline.get(AppShareHandler.class);
        return handler == null ? null : handler.state;
    }

    public static boolean needSync(Channel channel, String sharedToken) {
        BaseSimpleChannelInboundHandler appHandler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return appHandler != null && appHandler.state.contains(sharedToken);
    }

    //use only for rare cases
    public static User getStateUser(Channel channel) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return handler.state.user;
    }

}

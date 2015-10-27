package cc.blynk.server.utils;

import cc.blynk.server.handlers.app.AppHandler;
import cc.blynk.server.handlers.app.AppShareHandler;
import cc.blynk.server.handlers.app.auth.AppStateHolder;
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

    public static HardwareStateHolder getHardState(ChannelPipeline pipeline) {
        HardwareHandler handler = pipeline.get(HardwareHandler.class);
        return handler == null ? null : handler.state;
    }

    public static AppStateHolder getAppState(Channel channel) {
        return getAppState(channel.pipeline());
    }

    public static AppStateHolder getAppState(ChannelPipeline pipeline) {
        AppHandler handler = pipeline.get(AppHandler.class);
        return handler == null ? null : handler.state;
    }

    //use only for rare cases
    public static User getStateUser(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        HardwareHandler hardwareHandler = pipeline.get(HardwareHandler.class);
        if (hardwareHandler != null) {
            return hardwareHandler.state.user;
        }
        AppHandler appHandler = pipeline.get(AppHandler.class);
        if (appHandler != null) {
            return appHandler.state.user;
        }

        return pipeline.get(AppShareHandler.class).state.user;
    }

}

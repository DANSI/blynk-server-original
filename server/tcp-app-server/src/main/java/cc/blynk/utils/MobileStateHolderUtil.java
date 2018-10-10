package cc.blynk.utils;

import cc.blynk.server.application.handlers.main.MobileHandler;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.application.handlers.sharing.MobileShareHandler;
import cc.blynk.server.application.handlers.sharing.auth.MobileShareStateHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.01.16.
 */
public final class MobileStateHolderUtil {

    private MobileStateHolderUtil() {
    }

    public static MobileStateHolder getAppState(Channel channel) {
        return getAppState(channel.pipeline());
    }

    private static MobileStateHolder getAppState(ChannelPipeline pipeline) {
        MobileHandler handler = pipeline.get(MobileHandler.class);
        if (handler == null) {
            return getShareState(pipeline);
        }
        return handler.state;
    }

    public static MobileShareStateHolder getShareState(Channel channel) {
        return getShareState(channel.pipeline());
    }

    private static MobileShareStateHolder getShareState(ChannelPipeline pipeline) {
        MobileShareHandler handler = pipeline.get(MobileShareHandler.class);
        return handler == null ? null : handler.state;
    }


}

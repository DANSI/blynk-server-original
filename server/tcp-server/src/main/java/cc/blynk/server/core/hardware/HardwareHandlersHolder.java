package cc.blynk.server.core.hardware;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.HardwareHandler;
import cc.blynk.server.handlers.common.PingHandler;
import cc.blynk.server.handlers.hardware.HardwareLoginHandler;
import cc.blynk.server.handlers.hardware.TweetHandler;
import cc.blynk.server.twitter.TwitterWrapper;
import io.netty.channel.ChannelHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/27/2015.
 */
class HardwareHandlersHolder {

    private final BaseSimpleChannelInboundHandler[] baseHandlers;
    private final ChannelHandler[] allHandlers;

    public HardwareHandlersHolder(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(userRegistry, sessionsHolder);
        HardwareHandler hardwareHandler = new HardwareHandler(props, userRegistry, sessionsHolder);
        PingHandler pingHandler = new PingHandler(props, userRegistry, sessionsHolder);
        TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, new TwitterWrapper());

        this.baseHandlers = new BaseSimpleChannelInboundHandler[] {
                hardwareHandler,
                pingHandler,
                tweetHandler
        };

        this.allHandlers = new ChannelHandler[] {
                hardwareLoginHandler,
                hardwareHandler,
                pingHandler,
                tweetHandler
        };
    }

    public BaseSimpleChannelInboundHandler[] getBaseHandlers() {
        return baseHandlers;
    }

    public ChannelHandler[] getAllHandlers() {
        return allHandlers;
    }

}

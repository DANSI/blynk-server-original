package cc.blynk.server.core.hardware;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.HardwareHandler;
import cc.blynk.server.handlers.common.PingHandler;
import cc.blynk.server.handlers.hardware.EmailHandler;
import cc.blynk.server.handlers.hardware.HardwareLoginHandler;
import cc.blynk.server.handlers.hardware.TweetHandler;
import cc.blynk.server.handlers.hardware.notifications.NotificationBase;
import io.netty.channel.ChannelHandler;

import java.util.Queue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/27/2015.
 */
class HardwareHandlersHolder {

    private final BaseSimpleChannelInboundHandler[] baseHandlers;
    private final ChannelHandler[] allHandlers;

    public HardwareHandlersHolder(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                                 Queue<NotificationBase> notificationsQueue) {
        HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(userRegistry, sessionsHolder);
        HardwareHandler hardwareHandler = new HardwareHandler(props, userRegistry, sessionsHolder);
        PingHandler pingHandler = new PingHandler(props, userRegistry, sessionsHolder);


        //notification handlers
        TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsQueue);
        EmailHandler emailHandler = new EmailHandler(props, userRegistry, sessionsHolder, notificationsQueue);

        this.baseHandlers = new BaseSimpleChannelInboundHandler[] {
                hardwareHandler,
                pingHandler,
                tweetHandler,
                emailHandler
        };

        this.allHandlers = new ChannelHandler[] {
                hardwareLoginHandler,
                hardwareHandler,
                pingHandler,
                tweetHandler,
                emailHandler
        };
    }

    public BaseSimpleChannelInboundHandler[] getBaseHandlers() {
        return baseHandlers;
    }

    public ChannelHandler[] getAllHandlers() {
        return allHandlers;
    }

}

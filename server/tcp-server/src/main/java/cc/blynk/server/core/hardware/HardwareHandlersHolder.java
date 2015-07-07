package cc.blynk.server.core.hardware;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.dao.graph.Storage;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.BridgeHandler;
import cc.blynk.server.handlers.common.HardwareHandler;
import cc.blynk.server.handlers.common.PingHandler;
import cc.blynk.server.handlers.hardware.HardwareLoginHandler;
import cc.blynk.server.handlers.hardware.MailHandler;
import cc.blynk.server.handlers.hardware.PushHandler;
import cc.blynk.server.handlers.hardware.TweetHandler;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/27/2015.
 */
public class HardwareHandlersHolder {

    private final BaseSimpleChannelInboundHandler[] baseHandlers;
    private final ChannelHandler[] allHandlers;
    private final NotificationsProcessor notificationsProcessor;

    public HardwareHandlersHolder(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                                  NotificationsProcessor notificationsProcessor, Storage storage) {
        HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(userRegistry, sessionsHolder);
        HardwareHandler hardwareHandler = new HardwareHandler(props, userRegistry, sessionsHolder, storage);
        BridgeHandler bridgeHandler = new BridgeHandler(props, userRegistry, sessionsHolder);
        PingHandler pingHandler = new PingHandler(props, userRegistry, sessionsHolder);


        //notification handlers
        TweetHandler tweetHandler = new TweetHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
        MailHandler emailHandler = new MailHandler(props, userRegistry, sessionsHolder, notificationsProcessor);
        PushHandler pushHandler = new PushHandler(props, userRegistry, sessionsHolder, notificationsProcessor);

        this.notificationsProcessor = notificationsProcessor;

        this.baseHandlers = new BaseSimpleChannelInboundHandler[] {
                hardwareHandler,
                bridgeHandler,
                pingHandler,
                tweetHandler,
                emailHandler,
                pushHandler
        };

        this.allHandlers = new ChannelHandler[] {
                hardwareLoginHandler,
                hardwareHandler,
                bridgeHandler,
                pingHandler,
                tweetHandler,
                emailHandler,
                pushHandler
        };
    }

    public BaseSimpleChannelInboundHandler[] getBaseHandlers() {
        return baseHandlers;
    }

    public ChannelHandler[] getAllHandlers() {
        return allHandlers;
    }

    public NotificationsProcessor getNotificationsProcessor() {
        return notificationsProcessor;
    }
}

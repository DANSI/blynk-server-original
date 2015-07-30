package cc.blynk.server.core.hardware;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.server.storage.StorageDao;
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
                                  NotificationsProcessor notificationsProcessor, StorageDao storageDao) {
        HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(userRegistry, sessionsHolder);
        HardwareHandler hardwareHandler = new HardwareHandler(props, sessionsHolder, storageDao, notificationsProcessor);

        this.notificationsProcessor = notificationsProcessor;

        this.baseHandlers = new BaseSimpleChannelInboundHandler[] {
                hardwareHandler
        };

        this.allHandlers = new ChannelHandler[] {
                hardwareLoginHandler,
                hardwareHandler
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

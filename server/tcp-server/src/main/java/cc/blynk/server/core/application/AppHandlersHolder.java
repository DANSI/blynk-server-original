package cc.blynk.server.core.application;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.AppHandler;
import cc.blynk.server.handlers.app.auth.AppLoginHandler;
import cc.blynk.server.handlers.app.auth.RegisterHandler;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 3/10/2015.
 */
class AppHandlersHolder {

    private final BaseSimpleChannelInboundHandler[] baseHandlers;
    private final ChannelHandler[] allHandlers;

    public AppHandlersHolder(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder, StorageDao storageDao) {
        RegisterHandler registerHandler = new RegisterHandler(userRegistry);
        AppLoginHandler appLoginHandler = new AppLoginHandler(userRegistry, sessionsHolder);
        AppHandler appHandler = new AppHandler(props, userRegistry, sessionsHolder, storageDao);

        this.baseHandlers = new BaseSimpleChannelInboundHandler[] {
            appHandler
        };

        this.allHandlers = new ChannelHandler[] {
            registerHandler,
            appLoginHandler,
            appHandler
        };
    }

    public BaseSimpleChannelInboundHandler[] getBaseHandlers() {
        return baseHandlers;
    }

    public ChannelHandler[] getAllHandlers() {
        return allHandlers;
    }
}

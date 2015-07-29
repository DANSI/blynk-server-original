package cc.blynk.server.core.application;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.*;
import cc.blynk.server.handlers.app.auth.AppLoginHandler;
import cc.blynk.server.handlers.app.auth.RegisterHandler;
import cc.blynk.server.handlers.common.PingLogic;
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
        GetTokenHandler getTokenHandler = new GetTokenHandler(props, userRegistry, sessionsHolder);
        GetGraphDataHandler getGraphDataHandler = new GetGraphDataHandler(props, userRegistry, sessionsHolder, storageDao);
        RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(props, userRegistry, sessionsHolder);
        LoadProfileHandler loadProfileHandler = new LoadProfileHandler(props, userRegistry, sessionsHolder);
        SaveProfileHandler saveProfileHandler = new SaveProfileHandler(props, userRegistry, sessionsHolder);
        HardwareAppHandler hardwareHandler = new HardwareAppHandler(props, userRegistry, sessionsHolder);
        PingLogic pingHandler = new PingLogic(props, userRegistry, sessionsHolder);
        ActivateDashboardHandler activateDashboardHandler = new ActivateDashboardHandler(props, userRegistry, sessionsHolder);
        DeActivateDashboardHandler deActivateDashboardHandler = new DeActivateDashboardHandler(props, userRegistry, sessionsHolder);

        this.baseHandlers = new BaseSimpleChannelInboundHandler[] {
            getTokenHandler,
            refreshTokenHandler,
            loadProfileHandler,
            saveProfileHandler,
            hardwareHandler,
            pingHandler,
            activateDashboardHandler,
            deActivateDashboardHandler,
            getGraphDataHandler
        };

        this.allHandlers = new ChannelHandler[] {
            registerHandler,
            appLoginHandler,
            getTokenHandler,
            refreshTokenHandler,
            loadProfileHandler,
            saveProfileHandler,
            hardwareHandler,
            pingHandler,
            activateDashboardHandler,
            deActivateDashboardHandler,
            getGraphDataHandler
        };
    }

    public BaseSimpleChannelInboundHandler[] getBaseHandlers() {
        return baseHandlers;
    }

    public ChannelHandler[] getAllHandlers() {
        return allHandlers;
    }
}

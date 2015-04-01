package cc.blynk.server.core.application;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.*;
import cc.blynk.server.handlers.app.auth.AppLoginHandler;
import cc.blynk.server.handlers.app.auth.RegisterHandler;
import cc.blynk.server.handlers.common.HardwareHandler;
import cc.blynk.server.handlers.common.PingHandler;
import io.netty.channel.ChannelHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 3/10/2015.
 */
class AppHandlersHolder {

    private final BaseSimpleChannelInboundHandler[] baseHandlers;
    private final ChannelHandler[] allHandlers;

    public AppHandlersHolder(ServerProperties props, FileManager fileManager, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        RegisterHandler registerHandler = new RegisterHandler(fileManager, userRegistry, sessionsHolder);
        AppLoginHandler appLoginHandler = new AppLoginHandler(fileManager, userRegistry, sessionsHolder);
        GetTokenHandler getTokenHandler = new GetTokenHandler(props, fileManager, userRegistry, sessionsHolder);
        RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(props, fileManager, userRegistry, sessionsHolder);
        LoadProfileHandler loadProfileHandler = new LoadProfileHandler(props, fileManager, userRegistry, sessionsHolder);
        SaveProfileHandler saveProfileHandler = new SaveProfileHandler(props, fileManager, userRegistry, sessionsHolder);
        HardwareHandler hardwareHandler = new HardwareHandler(props, fileManager, userRegistry, sessionsHolder);
        PingHandler pingHandler = new PingHandler(props, fileManager, userRegistry, sessionsHolder);
        ActivateDashboardHandler activateDashboardHandler = new ActivateDashboardHandler(props, fileManager, userRegistry, sessionsHolder);
        DeActivateDashboardHandler deActivateDashboardHandler = new DeActivateDashboardHandler(props, fileManager, userRegistry, sessionsHolder);

        this.baseHandlers = new BaseSimpleChannelInboundHandler[] {
            getTokenHandler,
            refreshTokenHandler,
            loadProfileHandler,
            saveProfileHandler,
            hardwareHandler,
            pingHandler,
            activateDashboardHandler,
            deActivateDashboardHandler
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
            deActivateDashboardHandler
        };
    }

    public BaseSimpleChannelInboundHandler[] getBaseHandlers() {
        return baseHandlers;
    }

    public ChannelHandler[] getAllHandlers() {
        return allHandlers;
    }
}

package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.logic.*;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AppHandler extends BaseSimpleChannelInboundHandler<Message> {

    private final SaveProfileLogic saveProfile;
    private final GetTokenLogic token;
    private final HardwareAppLogic hardwareApp;
    private final RefreshTokenLogic refreshToken;
    private final GetGraphDataLogic graphData;
    private final AppMailLogic appMailLogic;

    public AppHandler(ServerProperties props, UserDao userDao, SessionDao sessionDao, ReportingDao reportingDao, NotificationsProcessor notificationsProcessor, HandlerState state) {
        super(state);
        this.saveProfile = new SaveProfileLogic(props);
        this.token = new GetTokenLogic(userDao);
        this.hardwareApp = new HardwareAppLogic(sessionDao);
        this.refreshToken = new RefreshTokenLogic(userDao);
        this.graphData = new GetGraphDataLogic(reportingDao);
        this.appMailLogic = new AppMailLogic(notificationsProcessor);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HandlerState state, Message msg) {
        switch (msg.command) {
            case HARDWARE:
                hardwareApp.messageReceived(ctx, state.user, msg);
                break;
            case SAVE_PROFILE :
                saveProfile.messageReceived(ctx, state.user, msg);
                break;
            case ACTIVATE_DASHBOARD :
                ActivateDashboardLogic.messageReceived(ctx, state.user, msg);
                break;
            case DEACTIVATE_DASHBOARD :
                DeActivateDashboardLogic.messageReceived(ctx, state.user, msg);
                break;
            case LOAD_PROFILE :
                LoadProfileLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_TOKEN :
                token.messageReceived(ctx, state.user, msg);
                break;
            case REFRESH_TOKEN :
                refreshToken.messageReceived(ctx, state.user, msg);
                break;
            case GET_GRAPH_DATA :
                graphData.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
            case EMAIL :
                appMailLogic.messageReceived(ctx, state.user, msg);
        }
    }

}

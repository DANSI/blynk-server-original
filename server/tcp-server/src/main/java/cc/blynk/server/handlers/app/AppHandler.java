package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.auth.AppStateHolder;
import cc.blynk.server.handlers.app.logic.*;
import cc.blynk.server.handlers.app.logic.dashboard.CreateDashLogic;
import cc.blynk.server.handlers.app.logic.dashboard.DeleteDashLogic;
import cc.blynk.server.handlers.app.logic.dashboard.SaveDashLogic;
import cc.blynk.server.handlers.app.logic.reporting.GetGraphDataLogic;
import cc.blynk.server.handlers.app.logic.sharing.GetShareTokenLogic;
import cc.blynk.server.handlers.app.logic.sharing.GetSharedDashLogic;
import cc.blynk.server.handlers.app.logic.sharing.RefreshShareTokenLogic;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.ThreadContext;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AppHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final AppStateHolder state;
    private final SaveProfileLogic saveProfile;
    private final GetTokenLogic token;
    private final HardwareAppLogic hardwareApp;
    private final RefreshTokenLogic refreshToken;
    private final GetGraphDataLogic graphData;
    private final AppMailLogic appMailLogic;
    private final GetShareTokenLogic getShareTokenLogic;
    private final RefreshShareTokenLogic refreshShareTokenLogic;
    private final GetSharedDashLogic getSharedDashLogic;
    private final CreateDashLogic createDashLogic;
    private final SaveDashLogic saveDashLogic;
    private final ActivateDashboardLogic activateDashboardLogic;

    public AppHandler(ServerProperties props, UserDao userDao, SessionDao sessionDao, ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor, AppStateHolder state) {
        super(props);
        this.saveProfile = new SaveProfileLogic(props);
        this.token = new GetTokenLogic(userDao);
        this.hardwareApp = new HardwareAppLogic(sessionDao);
        this.refreshToken = new RefreshTokenLogic(userDao);
        this.graphData = new GetGraphDataLogic(reportingDao, blockingIOProcessor);
        this.appMailLogic = new AppMailLogic(blockingIOProcessor);
        this.getShareTokenLogic = new GetShareTokenLogic(userDao);
        this.refreshShareTokenLogic = new RefreshShareTokenLogic(userDao);
        this.getSharedDashLogic = new GetSharedDashLogic(userDao);
        this.createDashLogic = new CreateDashLogic(props);
        this.saveDashLogic = new SaveDashLogic(props);
        this.activateDashboardLogic = new ActivateDashboardLogic(sessionDao);
        this.state = state;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        ThreadContext.put("user", state.user.name);
        switch (msg.command) {
            case HARDWARE:
                hardwareApp.messageReceived(ctx, state, msg);
                break;
            case SAVE_PROFILE :
                saveProfile.messageReceived(ctx, state.user, msg);
                break;
            case ACTIVATE_DASHBOARD :
                activateDashboardLogic.messageReceived(ctx, state.user, msg);
                break;
            case DEACTIVATE_DASHBOARD :
                DeActivateDashboardLogic.messageReceived(ctx, state.user, msg);
                break;
            case LOAD_PROFILE :
                LoadProfileLogic.messageReceived(ctx, state.user, msg);
                break;
            case LOAD_PROFILE_GZIPPED :
                LoadProfileGzippedLogic.messageReceived(ctx, state.user, msg);
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
            case GET_SHARE_TOKEN :
                getShareTokenLogic.messageReceived(ctx, state.user, msg);
                break;
            case REFRESH_SHARE_TOKEN :
                refreshShareTokenLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_SHARED_DASH :
                getSharedDashLogic.messageReceived(ctx, msg);
                break;
            case EMAIL :
                appMailLogic.messageReceived(ctx, state.user, msg);
                break;
            case CREATE_DASH :
                createDashLogic.messageReceived(ctx, state.user, msg);
                break;
            case SAVE_DASH :
                saveDashLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_DASH :
                DeleteDashLogic.messageReceived(ctx, state.user, msg);
                break;
        }
    }

}

package cc.blynk.server.application.handlers.main;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.application.handlers.main.logic.ActivateDashboardLogic;
import cc.blynk.server.application.handlers.main.logic.AddEnergyLogic;
import cc.blynk.server.application.handlers.main.logic.AddPushLogic;
import cc.blynk.server.application.handlers.main.logic.AppMailLogic;
import cc.blynk.server.application.handlers.main.logic.AppSyncLogic;
import cc.blynk.server.application.handlers.main.logic.AssignTokenLogic;
import cc.blynk.server.application.handlers.main.logic.CreateAppLogic;
import cc.blynk.server.application.handlers.main.logic.DeActivateDashboardLogic;
import cc.blynk.server.application.handlers.main.logic.DeleteAppLogic;
import cc.blynk.server.application.handlers.main.logic.GetCloneCodeLogic;
import cc.blynk.server.application.handlers.main.logic.GetEnergyLogic;
import cc.blynk.server.application.handlers.main.logic.GetProjectByClonedTokenLogic;
import cc.blynk.server.application.handlers.main.logic.GetProjectByTokenLogic;
import cc.blynk.server.application.handlers.main.logic.GetTokenLogic;
import cc.blynk.server.application.handlers.main.logic.HardwareAppLogic;
import cc.blynk.server.application.handlers.main.logic.HardwareResendFromBTLogic;
import cc.blynk.server.application.handlers.main.logic.LoadProfileGzippedLogic;
import cc.blynk.server.application.handlers.main.logic.MailQRsLogic;
import cc.blynk.server.application.handlers.main.logic.RedeemLogic;
import cc.blynk.server.application.handlers.main.logic.RefreshTokenLogic;
import cc.blynk.server.application.handlers.main.logic.UpdateAppLogic;
import cc.blynk.server.application.handlers.main.logic.UpdateFaceLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.CreateDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.DeleteDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.UpdateDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.UpdateDashSettingLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.CreateDeviceLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.DeleteDeviceLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.GetDevicesLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.UpdateDeviceLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.tags.CreateTagLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.tags.DeleteTagLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.tags.GetTagsLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.tags.UpdateTagLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.CreateWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.DeleteWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.GetWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.UpdateWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile.CreateTileTemplateLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile.DeleteTileTemplateLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile.UpdateTileTemplateLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.DeleteEnhancedGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.ExportGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.GetEnhancedGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.GetGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.GetShareTokenLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.RefreshShareTokenLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.ShareLogic;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.StateHolderBase;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.LogoutLogic;
import cc.blynk.server.handlers.common.PingLogic;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.ACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.ADD_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.ADD_PUSH_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.ASSIGN_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_APP;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.DEACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_APP;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL_QR;
import static cc.blynk.server.core.protocol.enums.Command.EXPORT_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_DEVICES;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_SHARE_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_TAGS;
import static cc.blynk.server.core.protocol.enums.Command.GET_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_RESEND_FROM_BLUETOOTH;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.enums.Command.LOGOUT;
import static cc.blynk.server.core.protocol.enums.Command.PING;
import static cc.blynk.server.core.protocol.enums.Command.REDEEM;
import static cc.blynk.server.core.protocol.enums.Command.REFRESH_SHARE_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.REFRESH_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.SHARING;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_APP;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_FACE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_PROJECT_SETTINGS;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_WIDGET;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AppHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final AppStateHolder state;
    private final GetTokenLogic token;
    private final AssignTokenLogic assignTokenLogic;
    private final HardwareAppLogic hardwareApp;
    private final HardwareResendFromBTLogic hardwareResendFromBTLogic;
    private final RefreshTokenLogic refreshToken;
    private final GetGraphDataLogic graphData;
    private final GetEnhancedGraphDataLogic enhancedGraphDataLogic;
    private final DeleteEnhancedGraphDataLogic deleteEnhancedGraphDataLogic;
    private final ExportGraphDataLogic exportGraphData;
    private final AppMailLogic appMailLogic;
    private final GetShareTokenLogic getShareTokenLogic;
    private final RefreshShareTokenLogic refreshShareTokenLogic;
    private final CreateDashLogic createDashLogic;
    private final UpdateDashLogic updateDashLogic;
    private final UpdateDashSettingLogic updateDashSettingLogic;
    private final ActivateDashboardLogic activateDashboardLogic;
    private final DeActivateDashboardLogic deActivateDashboardLogic;
    private final CreateWidgetLogic createWidgetLogic;
    private final UpdateWidgetLogic updateWidgetLogic;
    private final DeleteWidgetLogic deleteWidgetLogic;
    private final DeleteDashLogic deleteDashLogic;
    private final ShareLogic shareLogic;
    private final RedeemLogic redeemLogic;
    private final AddEnergyLogic addEnergyLogic;
    private final CreateDeviceLogic createDeviceLogic;
    private final DeleteDeviceLogic deleteDeviceLogic;
    private final LoadProfileGzippedLogic loadProfileGzippedLogic;
    private final CreateAppLogic createAppLogic;
    private final UpdateAppLogic updateAppLogic;
    private final GetProjectByTokenLogic getProjectByTokenLogic;
    private final MailQRsLogic mailQRsLogic;
    private final UpdateFaceLogic updateFaceLogic;
    private final GetCloneCodeLogic getCloneCodeLogic;
    private final GetProjectByClonedTokenLogic getProjectByCloneCodeLogic;

    private final GlobalStats stats;

    public AppHandler(Holder holder, AppStateHolder state) {
        super(StringMessage.class, holder.limits);
        this.token = new GetTokenLogic(holder);
        this.assignTokenLogic = new AssignTokenLogic(holder);
        this.hardwareApp = new HardwareAppLogic(holder, state.user.email);
        this.hardwareResendFromBTLogic = new HardwareResendFromBTLogic(holder, state.user.email);
        this.refreshToken = new RefreshTokenLogic(holder);
        this.graphData = new GetGraphDataLogic(holder.reportingDao, holder.blockingIOProcessor);
        this.enhancedGraphDataLogic = new GetEnhancedGraphDataLogic(holder.reportingDao, holder.blockingIOProcessor);
        this.deleteEnhancedGraphDataLogic = new DeleteEnhancedGraphDataLogic(
                holder.reportingDao, holder.blockingIOProcessor);
        this.exportGraphData = new ExportGraphDataLogic(holder);
        this.appMailLogic = new AppMailLogic(holder);
        this.getShareTokenLogic = new GetShareTokenLogic(holder.tokenManager);
        this.refreshShareTokenLogic = new RefreshShareTokenLogic(holder.tokenManager, holder.sessionDao);

        this.createDashLogic = new CreateDashLogic(holder.timerWorker,
                holder.tokenManager, holder.limits.dashboardsLimit, holder.limits.profileSizeLimitBytes);
        this.updateDashLogic = new UpdateDashLogic(holder.timerWorker, holder.limits.profileSizeLimitBytes);

        this.activateDashboardLogic = new ActivateDashboardLogic(holder.sessionDao);
        this.deActivateDashboardLogic = new DeActivateDashboardLogic(holder.sessionDao);

        this.createWidgetLogic = new CreateWidgetLogic(holder.limits.widgetSizeLimitBytes, holder.timerWorker);
        this.updateWidgetLogic = new UpdateWidgetLogic(holder.limits.widgetSizeLimitBytes, holder.timerWorker);
        this.deleteWidgetLogic = new DeleteWidgetLogic(holder.timerWorker);
        this.deleteDashLogic = new DeleteDashLogic(holder);
        this.updateDashSettingLogic = new UpdateDashSettingLogic(holder.limits.widgetSizeLimitBytes);

        this.createDeviceLogic = new CreateDeviceLogic(holder);
        this.deleteDeviceLogic = new DeleteDeviceLogic(holder.tokenManager, holder.sessionDao);

        this.shareLogic = new ShareLogic(holder.sessionDao);
        this.redeemLogic = new RedeemLogic(holder.dbManager, holder.blockingIOProcessor);
        this.addEnergyLogic = new AddEnergyLogic(holder.dbManager, holder.blockingIOProcessor);

        this.createAppLogic = new CreateAppLogic(holder.limits.widgetSizeLimitBytes);
        this.updateAppLogic = new UpdateAppLogic(holder.limits.widgetSizeLimitBytes);

        this.loadProfileGzippedLogic = new LoadProfileGzippedLogic(holder);
        this.getProjectByTokenLogic = new GetProjectByTokenLogic(holder);
        this.mailQRsLogic = new MailQRsLogic(holder);
        this.updateFaceLogic = new UpdateFaceLogic(holder);

        this.getCloneCodeLogic = new GetCloneCodeLogic(holder);
        this.getProjectByCloneCodeLogic = new GetProjectByClonedTokenLogic(holder);

        this.state = state;
        this.stats = holder.stats;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        this.stats.incrementAppStat();
        switch (msg.command) {
            case HARDWARE :
                hardwareApp.messageReceived(ctx, state, msg);
                break;
            case HARDWARE_RESEND_FROM_BLUETOOTH :
                hardwareResendFromBTLogic.messageReceived(ctx, state, msg);
                break;
            case ACTIVATE_DASHBOARD :
                activateDashboardLogic.messageReceived(ctx, state, msg);
                break;
            case DEACTIVATE_DASHBOARD :
                deActivateDashboardLogic.messageReceived(ctx, state, msg);
                break;
            case LOAD_PROFILE_GZIPPED :
                loadProfileGzippedLogic.messageReceived(ctx, state, msg);
                break;
            case SHARING :
                shareLogic.messageReceived(ctx, state, msg);
                break;

            case GET_TOKEN :
                token.messageReceived(ctx, state.user, msg);
                break;
            case ASSIGN_TOKEN :
                assignTokenLogic.messageReceived(ctx, state.user, msg);
                break;
            case ADD_PUSH_TOKEN :
                AddPushLogic.messageReceived(ctx, state, msg);
                break;
            case REFRESH_TOKEN :
                refreshToken.messageReceived(ctx, state, msg);
                break;

            case GET_GRAPH_DATA :
                graphData.messageReceived(ctx, state.user, msg);
                break;
            case GET_ENHANCED_GRAPH_DATA :
                enhancedGraphDataLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_ENHANCED_GRAPH_DATA :
                deleteEnhancedGraphDataLogic.messageReceived(ctx, state.user, msg);
                break;
            case EXPORT_GRAPH_DATA :
                exportGraphData.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;

            case GET_SHARE_TOKEN :
                getShareTokenLogic.messageReceived(ctx, state.user, msg);
                break;
            case REFRESH_SHARE_TOKEN :
                refreshShareTokenLogic.messageReceived(ctx, state, msg);
                break;

            case EMAIL :
                appMailLogic.messageReceived(ctx, state.user, msg);
                break;

            case CREATE_DASH :
                createDashLogic.messageReceived(ctx, state, msg);
                break;
            case UPDATE_DASH:
                updateDashLogic.messageReceived(ctx, state, msg);
                break;
            case DELETE_DASH :
                deleteDashLogic.messageReceived(ctx, state, msg);
                break;

            case CREATE_WIDGET :
                createWidgetLogic.messageReceived(ctx, state, msg);
                break;
            case UPDATE_WIDGET :
                updateWidgetLogic.messageReceived(ctx, state, msg);
                break;
            case DELETE_WIDGET :
                deleteWidgetLogic.messageReceived(ctx, state, msg);
                break;
            case GET_WIDGET :
                GetWidgetLogic.messageReceived(ctx, state, msg);
                break;

            case CREATE_TILE_TEMPLATE :
                CreateTileTemplateLogic.messageReceived(ctx, state, msg);
                break;
            case UPDATE_TILE_TEMPLATE :
                UpdateTileTemplateLogic.messageReceived(ctx, state, msg);
                break;
            case DELETE_TILE_TEMPLATE :
                DeleteTileTemplateLogic.messageReceived(ctx, state, msg);
                break;

            case REDEEM :
                redeemLogic.messageReceived(ctx, state.user, msg);
                break;

            case GET_ENERGY :
                GetEnergyLogic.messageReceived(ctx, state.user, msg);
                break;
            case ADD_ENERGY :
                addEnergyLogic.messageReceived(ctx, state.user, msg);
                break;

            case UPDATE_PROJECT_SETTINGS :
                updateDashSettingLogic.messageReceived(ctx, state, msg);
                break;

            case CREATE_DEVICE :
                createDeviceLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_DEVICE :
                UpdateDeviceLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_DEVICE :
                deleteDeviceLogic.messageReceived(ctx, state, msg);
                break;
            case GET_DEVICES :
                GetDevicesLogic.messageReceived(ctx, state.user, msg);
                break;

            case CREATE_TAG :
                CreateTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_TAG :
                UpdateTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_TAG :
                DeleteTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_TAGS :
                GetTagsLogic.messageReceived(ctx, state.user, msg);
                break;

            case APP_SYNC :
                AppSyncLogic.messageReceived(ctx, state, msg);
                break;

            case CREATE_APP :
                createAppLogic.messageReceived(ctx, state, msg);
                break;
            case UPDATE_APP :
                updateAppLogic.messageReceived(ctx, state, msg);
                break;
            case DELETE_APP :
                DeleteAppLogic.messageReceived(ctx, state, msg);
                break;

            case GET_PROJECT_BY_TOKEN :
                getProjectByTokenLogic.messageReceived(ctx, state.user, msg);
                break;
            case EMAIL_QR :
                mailQRsLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_FACE :
                updateFaceLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_CLONE_CODE :
                getCloneCodeLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_PROJECT_BY_CLONE_CODE :
                getProjectByCloneCodeLogic.messageReceived(ctx, msg);
                break;
            case LOGOUT :
                LogoutLogic.messageReceived(ctx, state.user, msg);
                break;
        }
    }

    @Override
    public StateHolderBase getState() {
        return state;
    }
}

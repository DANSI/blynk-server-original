package cc.blynk.server.application.handlers.main;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.application.handlers.main.logic.ActivateDashboardLogic;
import cc.blynk.server.application.handlers.main.logic.AddPushLogic;
import cc.blynk.server.application.handlers.main.logic.AppMailLogic;
import cc.blynk.server.application.handlers.main.logic.AppSetWidgetPropertyLogic;
import cc.blynk.server.application.handlers.main.logic.AppSyncLogic;
import cc.blynk.server.application.handlers.main.logic.AssignTokenLogic;
import cc.blynk.server.application.handlers.main.logic.DeActivateDashboardLogic;
import cc.blynk.server.application.handlers.main.logic.GetCloneCodeLogic;
import cc.blynk.server.application.handlers.main.logic.GetEnergyLogic;
import cc.blynk.server.application.handlers.main.logic.GetProjectByClonedTokenLogic;
import cc.blynk.server.application.handlers.main.logic.GetProjectByTokenLogic;
import cc.blynk.server.application.handlers.main.logic.GetProvisionTokenLogic;
import cc.blynk.server.application.handlers.main.logic.GetTokenLogic;
import cc.blynk.server.application.handlers.main.logic.HardwareAppLogic;
import cc.blynk.server.application.handlers.main.logic.HardwareResendFromBTLogic;
import cc.blynk.server.application.handlers.main.logic.LoadProfileGzippedLogic;
import cc.blynk.server.application.handlers.main.logic.LogoutLogic;
import cc.blynk.server.application.handlers.main.logic.PurchaseLogic;
import cc.blynk.server.application.handlers.main.logic.RedeemLogic;
import cc.blynk.server.application.handlers.main.logic.RefreshTokenLogic;
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
import cc.blynk.server.application.handlers.main.logic.face.CreateAppLogic;
import cc.blynk.server.application.handlers.main.logic.face.DeleteAppLogic;
import cc.blynk.server.application.handlers.main.logic.face.MailQRsLogic;
import cc.blynk.server.application.handlers.main.logic.face.UpdateAppLogic;
import cc.blynk.server.application.handlers.main.logic.face.UpdateFaceLogic;
import cc.blynk.server.application.handlers.main.logic.graph.DeleteDeviceDataLogic;
import cc.blynk.server.application.handlers.main.logic.graph.DeleteEnhancedGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.graph.ExportGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.graph.GetEnhancedGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.CreateReportLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.DeleteReportLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.ExportReportLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.UpdateReportLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.GetShareTokenLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.RefreshShareTokenLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.ShareLogic;
import cc.blynk.server.common.BaseSimpleChannelInboundHandler;
import cc.blynk.server.common.handlers.logic.PingLogic;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.StateHolderBase;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.ACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.ADD_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.ADD_PUSH_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.ASSIGN_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_APP;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_REPORT;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.DEACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_APP;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DEVICE_DATA;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_REPORT;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_TILE_TEMPLATE;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL_QR;
import static cc.blynk.server.core.protocol.enums.Command.EXPORT_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.EXPORT_REPORT;
import static cc.blynk.server.core.protocol.enums.Command.GET_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_DEVICES;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROVISION_TOKEN;
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
import static cc.blynk.server.core.protocol.enums.Command.SET_WIDGET_PROPERTY;
import static cc.blynk.server.core.protocol.enums.Command.SHARING;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_APP;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_DASH;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_FACE;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_PROJECT_SETTINGS;
import static cc.blynk.server.core.protocol.enums.Command.UPDATE_REPORT;
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
    private final Holder holder;
    private final HardwareAppLogic hardwareApp;

    private HardwareResendFromBTLogic hardwareResendFromBTLogic;
    private ExportGraphDataLogic exportGraphData;
    private AppMailLogic appMailLogic;
    private PurchaseLogic purchaseLogic;
    private DeleteAppLogic deleteAppLogic;
    private MailQRsLogic mailQRsLogic;
    private GetProjectByClonedTokenLogic getProjectByCloneCodeLogic;

    public AppHandler(Holder holder, AppStateHolder state) {
        super(StringMessage.class);
        this.state = state;
        this.holder = holder;

        this.hardwareApp = new HardwareAppLogic(holder, state.user.email);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        holder.stats.incrementAppStat();
        switch (msg.command) {
            case HARDWARE :
                hardwareApp.messageReceived(ctx, state, msg);
                break;
            case HARDWARE_RESEND_FROM_BLUETOOTH :
                if (hardwareResendFromBTLogic == null) {
                    this.hardwareResendFromBTLogic = new HardwareResendFromBTLogic(holder, state.user.email);
                }
                hardwareResendFromBTLogic.messageReceived(ctx, state, msg);
                break;
            case ACTIVATE_DASHBOARD :
                ActivateDashboardLogic.messageReceived(holder, ctx, state, msg);
                break;
            case DEACTIVATE_DASHBOARD :
                DeActivateDashboardLogic.messageReceived(holder, ctx, state, msg);
                break;
            case LOAD_PROFILE_GZIPPED :
                LoadProfileGzippedLogic.messageReceived(holder, ctx, state, msg);
                break;
            case SHARING :
                ShareLogic.messageReceived(holder, ctx, state, msg);
                break;

            case GET_TOKEN :
                GetTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case ASSIGN_TOKEN :
                AssignTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case ADD_PUSH_TOKEN :
                AddPushLogic.messageReceived(ctx, state, msg);
                break;
            case REFRESH_TOKEN :
                RefreshTokenLogic.messageReceived(holder, ctx, state, msg);
                break;

            case GET_ENHANCED_GRAPH_DATA :
                GetEnhancedGraphDataLogic.messageReceived(holder, ctx, state, msg);
                break;
            case DELETE_ENHANCED_GRAPH_DATA :
                DeleteEnhancedGraphDataLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case EXPORT_GRAPH_DATA :
                if (exportGraphData == null) {
                    this.exportGraphData = new ExportGraphDataLogic(holder);
                }
                exportGraphData.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;

            case GET_SHARE_TOKEN :
                GetShareTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case REFRESH_SHARE_TOKEN :
                RefreshShareTokenLogic.messageReceived(holder, ctx, state, msg);
                break;

            case EMAIL :
                if (appMailLogic == null) {
                    this.appMailLogic = new AppMailLogic(holder);
                }
                appMailLogic.messageReceived(ctx, state.user, msg);
                break;

            case CREATE_DASH :
                CreateDashLogic.messageReceived(holder, ctx, state, msg);
                break;
            case UPDATE_DASH:
                UpdateDashLogic.messageReceived(holder, ctx, state, msg);
                break;
            case DELETE_DASH :
                DeleteDashLogic.messageReceived(holder, ctx, state, msg);
                break;

            case CREATE_WIDGET :
                CreateWidgetLogic.messageReceived(holder, ctx, state, msg);
                break;
            case UPDATE_WIDGET :
                UpdateWidgetLogic.messageReceived(holder, ctx, state, msg);
                break;
            case DELETE_WIDGET :
                DeleteWidgetLogic.messageReceived(holder, ctx, state, msg);
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
                RedeemLogic.messageReceived(holder, ctx, state.user, msg);
                break;

            case GET_ENERGY :
                GetEnergyLogic.messageReceived(ctx, state.user, msg);
                break;
            case ADD_ENERGY :
                if (purchaseLogic == null) {
                    this.purchaseLogic = new PurchaseLogic(holder);
                }
                purchaseLogic.messageReceived(ctx, state, msg);
                break;

            case UPDATE_PROJECT_SETTINGS :
                UpdateDashSettingLogic.messageReceived(ctx, state, msg, holder.limits.widgetSizeLimitBytes);
                break;

            case CREATE_DEVICE :
                CreateDeviceLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case UPDATE_DEVICE :
                UpdateDeviceLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_DEVICE :
                DeleteDeviceLogic.messageReceived(holder, ctx, state, msg);
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
                CreateAppLogic.messageReceived(ctx, state, msg, holder.limits.widgetSizeLimitBytes);
                break;
            case UPDATE_APP :
                UpdateAppLogic.messageReceived(ctx, state, msg, holder.limits.widgetSizeLimitBytes);
                break;
            case DELETE_APP :
                if (deleteAppLogic == null) {
                    this.deleteAppLogic = new DeleteAppLogic(holder);
                }
                deleteAppLogic.messageReceived(ctx, state, msg);
                break;

            case GET_PROJECT_BY_TOKEN :
                GetProjectByTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case EMAIL_QR :
                if (mailQRsLogic == null) {
                    this.mailQRsLogic = new MailQRsLogic(holder);
                }
                mailQRsLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_FACE :
                UpdateFaceLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case GET_CLONE_CODE :
                GetCloneCodeLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case GET_PROJECT_BY_CLONE_CODE :
                if (getProjectByCloneCodeLogic == null) {
                    this.getProjectByCloneCodeLogic = new GetProjectByClonedTokenLogic(holder);
                }
                getProjectByCloneCodeLogic.messageReceived(ctx, state.user, msg);
                break;
            case LOGOUT :
                LogoutLogic.messageReceived(ctx, state.user, msg);
                break;
            case SET_WIDGET_PROPERTY :
                AppSetWidgetPropertyLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_PROVISION_TOKEN :
                GetProvisionTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case DELETE_DEVICE_DATA :
                DeleteDeviceDataLogic.messageReceived(holder, ctx, state, msg);
                break;
            case CREATE_REPORT :
                CreateReportLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case UPDATE_REPORT :
                UpdateReportLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case DELETE_REPORT :
                DeleteReportLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case EXPORT_REPORT :
                ExportReportLogic.messageReceived(holder, ctx, state.user, msg);
                break;
        }
    }

    @Override
    public StateHolderBase getState() {
        return state;
    }
}

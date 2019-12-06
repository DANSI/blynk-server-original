package cc.blynk.server.application.handlers.main;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.application.handlers.main.logic.MobileActivateDashboardLogic;
import cc.blynk.server.application.handlers.main.logic.MobileAddPushLogic;
import cc.blynk.server.application.handlers.main.logic.MobileAssignTokenLogic;
import cc.blynk.server.application.handlers.main.logic.MobileDeActivateDashboardLogic;
import cc.blynk.server.application.handlers.main.logic.MobileGetCloneCodeLogic;
import cc.blynk.server.application.handlers.main.logic.MobileGetEnergyLogic;
import cc.blynk.server.application.handlers.main.logic.MobileGetProjectByClonedTokenLogic;
import cc.blynk.server.application.handlers.main.logic.MobileGetProjectByTokenLogic;
import cc.blynk.server.application.handlers.main.logic.MobileGetProvisionTokenLogic;
import cc.blynk.server.application.handlers.main.logic.MobileHardwareLogic;
import cc.blynk.server.application.handlers.main.logic.MobileHardwareResendFromBTLogic;
import cc.blynk.server.application.handlers.main.logic.MobileLoadProfileGzippedLogic;
import cc.blynk.server.application.handlers.main.logic.MobileLogoutLogic;
import cc.blynk.server.application.handlers.main.logic.MobileMailLogic;
import cc.blynk.server.application.handlers.main.logic.MobilePurchaseLogic;
import cc.blynk.server.application.handlers.main.logic.MobileRedeemLogic;
import cc.blynk.server.application.handlers.main.logic.MobileRefreshTokenLogic;
import cc.blynk.server.application.handlers.main.logic.MobileSetWidgetPropertyLogic;
import cc.blynk.server.application.handlers.main.logic.MobileSyncLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.MobileCreateDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.MobileDeleteDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.MobileUpdateDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.MobileUpdateDashSettingLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.MobileCreateDeviceLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.MobileDeleteDeviceLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.MobileGetDeviceLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.MobileGetDevicesLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.MobileUpdateDeviceLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.tags.MobileCreateTagLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.tags.MobileDeleteTagLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.tags.MobileGetTagsLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.tags.MobileUpdateTagLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.MobileCreateWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.MobileDeleteWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.MobileGetWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.MobileUpdateWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile.MobileCreateTileTemplateLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile.MobileDeleteTileTemplateLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile.MobileUpdateTileTemplateLogic;
import cc.blynk.server.application.handlers.main.logic.face.MobileCreateAppLogic;
import cc.blynk.server.application.handlers.main.logic.face.MobileDeleteAppLogic;
import cc.blynk.server.application.handlers.main.logic.face.MobileMailQRsLogic;
import cc.blynk.server.application.handlers.main.logic.face.MobileUpdateAppLogic;
import cc.blynk.server.application.handlers.main.logic.face.MobileUpdateFaceLogic;
import cc.blynk.server.application.handlers.main.logic.graph.MobileDeleteDeviceDataLogic;
import cc.blynk.server.application.handlers.main.logic.graph.MobileDeleteEnhancedGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.graph.MobileExportGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.graph.MobileGetEnhancedGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.MobileCreateReportLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.MobileDeleteReportLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.MobileExportReportLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.MobileUpdateReportLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.MobileGetShareTokenLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.MobileRefreshShareTokenLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.MobileShareLogic;
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
import static cc.blynk.server.core.protocol.enums.Command.GET_WIDGET;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_RESEND_FROM_BLUETOOTH;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.enums.Command.LOGOUT;
import static cc.blynk.server.core.protocol.enums.Command.MOBILE_GET_DEVICE;
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
public class MobileHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final MobileStateHolder state;
    private final Holder holder;
    private final MobileHardwareLogic hardwareLogic;
    private final MobileAddPushLogic mobileAddPushLogic;

    private MobileHardwareResendFromBTLogic hardwareResendFromBTLogic;
    private MobileExportGraphDataLogic exportGraphData;
    private MobileMailLogic mailLogic;
    private MobilePurchaseLogic purchaseLogic;
    private MobileDeleteAppLogic deleteAppLogic;
    private MobileMailQRsLogic mailQRsLogic;
    private MobileGetProjectByClonedTokenLogic getProjectByCloneCodeLogic;

    public MobileHandler(Holder holder, MobileStateHolder state) {
        super(StringMessage.class);
        this.state = state;
        this.holder = holder;

        this.hardwareLogic = new MobileHardwareLogic(holder, state.user.email);
        this.mobileAddPushLogic = new MobileAddPushLogic(holder);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        holder.stats.incrementAppStat();
        switch (msg.command) {
            case HARDWARE :
                hardwareLogic.messageReceived(ctx, state, msg);
                break;
            case HARDWARE_RESEND_FROM_BLUETOOTH :
                if (hardwareResendFromBTLogic == null) {
                    this.hardwareResendFromBTLogic = new MobileHardwareResendFromBTLogic(holder, state.user.email);
                }
                hardwareResendFromBTLogic.messageReceived(ctx, state, msg);
                break;
            case ACTIVATE_DASHBOARD :
                MobileActivateDashboardLogic.messageReceived(holder, ctx, state, msg);
                break;
            case DEACTIVATE_DASHBOARD :
                MobileDeActivateDashboardLogic.messageReceived(holder, ctx, state, msg);
                break;
            case LOAD_PROFILE_GZIPPED :
                MobileLoadProfileGzippedLogic.messageReceived(holder, ctx, state, msg);
                break;
            case SHARING :
                MobileShareLogic.messageReceived(holder, ctx, state, msg);
                break;

            case ASSIGN_TOKEN :
                MobileAssignTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case ADD_PUSH_TOKEN :
                mobileAddPushLogic.messageReceived(ctx, state, msg);
                break;
            case REFRESH_TOKEN :
                MobileRefreshTokenLogic.messageReceived(holder, ctx, state, msg);
                break;

            case GET_ENHANCED_GRAPH_DATA :
                MobileGetEnhancedGraphDataLogic.messageReceived(holder, ctx, state, msg);
                break;
            case DELETE_ENHANCED_GRAPH_DATA :
                MobileDeleteEnhancedGraphDataLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case EXPORT_GRAPH_DATA :
                if (exportGraphData == null) {
                    this.exportGraphData = new MobileExportGraphDataLogic(holder);
                }
                exportGraphData.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;

            case GET_SHARE_TOKEN :
                MobileGetShareTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case REFRESH_SHARE_TOKEN :
                MobileRefreshShareTokenLogic.messageReceived(holder, ctx, state, msg);
                break;

            case EMAIL :
                if (mailLogic == null) {
                    this.mailLogic = new MobileMailLogic(holder);
                }
                mailLogic.messageReceived(ctx, state.user, msg);
                break;

            case CREATE_DASH :
                MobileCreateDashLogic.messageReceived(holder, ctx, state, msg);
                break;
            case UPDATE_DASH:
                MobileUpdateDashLogic.messageReceived(holder, ctx, state, msg);
                break;
            case DELETE_DASH :
                MobileDeleteDashLogic.messageReceived(holder, ctx, state, msg);
                break;

            case CREATE_WIDGET :
                MobileCreateWidgetLogic.messageReceived(holder, ctx, state, msg);
                break;
            case UPDATE_WIDGET :
                MobileUpdateWidgetLogic.messageReceived(holder, ctx, state, msg);
                break;
            case DELETE_WIDGET :
                MobileDeleteWidgetLogic.messageReceived(holder, ctx, state, msg);
                break;
            case GET_WIDGET :
                MobileGetWidgetLogic.messageReceived(ctx, state, msg);
                break;

            case CREATE_TILE_TEMPLATE :
                MobileCreateTileTemplateLogic.messageReceived(ctx, state, msg);
                break;
            case UPDATE_TILE_TEMPLATE :
                MobileUpdateTileTemplateLogic.messageReceived(ctx, state, msg);
                break;
            case DELETE_TILE_TEMPLATE :
                MobileDeleteTileTemplateLogic.messageReceived(ctx, state, msg);
                break;

            case REDEEM :
                MobileRedeemLogic.messageReceived(holder, ctx, state.user, msg);
                break;

            case GET_ENERGY :
                MobileGetEnergyLogic.messageReceived(ctx, state.user, msg);
                break;
            case ADD_ENERGY :
                if (purchaseLogic == null) {
                    this.purchaseLogic = new MobilePurchaseLogic(holder);
                }
                purchaseLogic.messageReceived(ctx, state, msg);
                break;

            case UPDATE_PROJECT_SETTINGS :
                MobileUpdateDashSettingLogic.messageReceived(ctx, state, msg, holder.limits.widgetSizeLimitBytes);
                break;

            case CREATE_DEVICE :
                MobileCreateDeviceLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case UPDATE_DEVICE :
                MobileUpdateDeviceLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_DEVICE :
                MobileDeleteDeviceLogic.messageReceived(holder, ctx, state, msg);
                break;
            case GET_DEVICES :
                MobileGetDevicesLogic.messageReceived(ctx, state.user, msg);
                break;
            case MOBILE_GET_DEVICE:
                MobileGetDeviceLogic.messageReceived(ctx, state.user, msg);
                break;

            case CREATE_TAG :
                MobileCreateTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_TAG :
                MobileUpdateTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_TAG :
                MobileDeleteTagLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_TAGS :
                MobileGetTagsLogic.messageReceived(ctx, state.user, msg);
                break;

            case APP_SYNC :
                MobileSyncLogic.messageReceived(ctx, state, msg);
                break;

            case CREATE_APP :
                MobileCreateAppLogic.messageReceived(ctx, state, msg, holder.limits.widgetSizeLimitBytes);
                break;
            case UPDATE_APP :
                MobileUpdateAppLogic.messageReceived(ctx, state, msg, holder.limits.widgetSizeLimitBytes);
                break;
            case DELETE_APP :
                if (deleteAppLogic == null) {
                    this.deleteAppLogic = new MobileDeleteAppLogic(holder);
                }
                deleteAppLogic.messageReceived(ctx, state, msg);
                break;

            case GET_PROJECT_BY_TOKEN :
                MobileGetProjectByTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case EMAIL_QR :
                if (mailQRsLogic == null) {
                    this.mailQRsLogic = new MobileMailQRsLogic(holder);
                }
                mailQRsLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_FACE :
                MobileUpdateFaceLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case GET_CLONE_CODE :
                MobileGetCloneCodeLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case GET_PROJECT_BY_CLONE_CODE :
                if (getProjectByCloneCodeLogic == null) {
                    this.getProjectByCloneCodeLogic = new MobileGetProjectByClonedTokenLogic(holder);
                }
                getProjectByCloneCodeLogic.messageReceived(ctx, state.user, msg);
                break;
            case LOGOUT :
                MobileLogoutLogic.messageReceived(ctx, state.user, msg);
                break;
            case SET_WIDGET_PROPERTY :
                MobileSetWidgetPropertyLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_PROVISION_TOKEN :
                MobileGetProvisionTokenLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case DELETE_DEVICE_DATA :
                MobileDeleteDeviceDataLogic.messageReceived(holder, ctx, state, msg);
                break;
            case CREATE_REPORT :
                MobileCreateReportLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case UPDATE_REPORT :
                MobileUpdateReportLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case DELETE_REPORT :
                MobileDeleteReportLogic.messageReceived(holder, ctx, state.user, msg);
                break;
            case EXPORT_REPORT :
                MobileExportReportLogic.messageReceived(holder, ctx, state.user, msg);
                break;
        }
    }

    @Override
    public StateHolderBase getState() {
        return state;
    }
}

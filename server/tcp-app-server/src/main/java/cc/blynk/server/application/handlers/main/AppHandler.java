package cc.blynk.server.application.handlers.main;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.application.handlers.main.logic.*;
import cc.blynk.server.application.handlers.main.logic.dashboard.CreateDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.DeleteDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.SaveDashLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.CreateWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.DeleteWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.widget.UpdateWidgetLogic;
import cc.blynk.server.application.handlers.main.logic.reporting.GetGraphDataLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.GetShareTokenLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.GetSharedDashLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.RefreshShareTokenLogic;
import cc.blynk.server.application.handlers.main.logic.sharing.ShareLogic;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.PingLogic;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.ThreadContext;

import static cc.blynk.server.core.protocol.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AppHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final AppStateHolder state;
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
    private final DeActivateDashboardLogic deActivateDashboardLogic;
    private final CreateWidgetLogic createWidgetLogic;
    private final UpdateWidgetLogic updateWidgetLogic;
    private final DeleteDashLogic deleteDashLogic;
    private final ShareLogic shareLogic;
    private final RedeemLogic redeemLogic;

    public AppHandler(Holder holder, AppStateHolder state) {
        super(holder.props, state);
        this.token = new GetTokenLogic(holder.userDao);
        this.hardwareApp = new HardwareAppLogic(holder.sessionDao);
        this.refreshToken = new RefreshTokenLogic(holder.userDao);
        this.graphData = new GetGraphDataLogic(holder.reportingDao, holder.blockingIOProcessor);
        this.appMailLogic = new AppMailLogic(holder.blockingIOProcessor, holder.mailWrapper);
        this.getShareTokenLogic = new GetShareTokenLogic(holder.userDao);
        this.refreshShareTokenLogic = new RefreshShareTokenLogic(holder.userDao, holder.sessionDao);
        this.getSharedDashLogic = new GetSharedDashLogic(holder.userDao);

        final int profileMaxSize = holder.props.getIntProperty("user.profile.max.size", 10) * 1024;
        this.createDashLogic = new CreateDashLogic(holder.props.getIntProperty("user.dashboard.max.limit"), profileMaxSize);
        this.saveDashLogic = new SaveDashLogic(profileMaxSize);

        this.activateDashboardLogic = new ActivateDashboardLogic(holder.sessionDao);
        this.deActivateDashboardLogic = new DeActivateDashboardLogic(holder.sessionDao);

        final int widgetSize = holder.props.getIntProperty("user.widget.max.size.limit", 10) * 1024;
        this.createWidgetLogic = new CreateWidgetLogic(widgetSize);
        this.updateWidgetLogic = new UpdateWidgetLogic(widgetSize);
        this.deleteDashLogic = new DeleteDashLogic(holder.userDao);

        this.shareLogic = new ShareLogic(holder.sessionDao);
        this.redeemLogic = new RedeemLogic(holder.dbManager, holder.blockingIOProcessor);

        this.state = state;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        if (state.user == null) {
            log.error("Should never happen.");
            return;
        }
        ThreadContext.put("user", state.user.name);
        switch (msg.command) {
            case HARDWARE:
                hardwareApp.messageReceived(ctx, state, msg);
                break;
            case ACTIVATE_DASHBOARD :
                activateDashboardLogic.messageReceived(ctx, state.user, msg);
                break;
            case DEACTIVATE_DASHBOARD :
                deActivateDashboardLogic.messageReceived(ctx, state.user, msg);
                break;
            case LOAD_PROFILE_GZIPPED :
                LoadProfileGzippedLogic.messageReceived(ctx, state.user, msg);
                break;
            case SHARING :
                shareLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_TOKEN :
                token.messageReceived(ctx, state.user, msg);
                break;
            case ADD_PUSH_TOKEN :
                AddPushLogic.messageReceived(ctx, state, msg);
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
                deleteDashLogic.messageReceived(ctx, state.user, msg);
                break;
            case CREATE_WIDGET :
                createWidgetLogic.messageReceived(ctx, state.user, msg);
                break;
            case UPDATE_WIDGET :
                updateWidgetLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_WIDGET :
                DeleteWidgetLogic.messageReceived(ctx, state.user, msg);
                break;
            case REDEEM :
                redeemLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_ENERGY :
                GetEnergyLogic.messageReceived(ctx, state.user, msg);
                break;
            case ADD_ENERGY :
                AddEnergyLogic.messageReceived(ctx, state.user, msg);
                break;

        }
    }

}

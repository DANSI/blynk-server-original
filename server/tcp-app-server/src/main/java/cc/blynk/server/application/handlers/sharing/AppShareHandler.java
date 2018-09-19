package cc.blynk.server.application.handlers.sharing;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.logic.AddPushLogic;
import cc.blynk.server.application.handlers.main.logic.AppSyncLogic;
import cc.blynk.server.application.handlers.main.logic.LoadSharedProfileGzippedLogic;
import cc.blynk.server.application.handlers.main.logic.LogoutLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.GetDevicesLogic;
import cc.blynk.server.application.handlers.main.logic.graph.DeleteDeviceDataLogic;
import cc.blynk.server.application.handlers.main.logic.graph.GetEnhancedGraphDataLogic;
import cc.blynk.server.application.handlers.sharing.auth.AppShareStateHolder;
import cc.blynk.server.application.handlers.sharing.logic.HardwareAppShareLogic;
import cc.blynk.server.common.BaseSimpleChannelInboundHandler;
import cc.blynk.server.common.handlers.logic.PingLogic;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.StateHolderBase;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.ADD_PUSH_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.DELETE_DEVICE_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_DEVICES;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.enums.Command.LOGOUT;
import static cc.blynk.server.core.protocol.enums.Command.PING;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AppShareHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final AppShareStateHolder state;
    private final Holder holder;
    private final HardwareAppShareLogic hardwareApp;

    public AppShareHandler(Holder holder, AppShareStateHolder state) {
        super(StringMessage.class);
        this.state = state;
        this.holder = holder;

        this.hardwareApp = new HardwareAppShareLogic(holder, state.userKey.email);

    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        holder.stats.incrementAppStat();
        switch (msg.command) {
            case HARDWARE:
                hardwareApp.messageReceived(ctx, state, msg);
                break;
            case LOAD_PROFILE_GZIPPED :
                LoadSharedProfileGzippedLogic.messageReceived(ctx, state, msg);
                break;
            case ADD_PUSH_TOKEN :
                AddPushLogic.messageReceived(ctx, state, msg);
                break;
            case GET_ENHANCED_GRAPH_DATA :
                GetEnhancedGraphDataLogic.messageReceived(holder, ctx, state, msg);
                break;
            case GET_DEVICES :
                GetDevicesLogic.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
            case APP_SYNC :
                AppSyncLogic.messageReceived(ctx, state, msg);
                break;
            case LOGOUT :
                LogoutLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_DEVICE_DATA :
                DeleteDeviceDataLogic.messageReceived(holder, ctx, state, msg);
                break;
        }
    }

    @Override
    public StateHolderBase getState() {
        return state;
    }
}

package cc.blynk.server.application.handlers.sharing;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.logic.LoadSharedProfileGzippedLogic;
import cc.blynk.server.application.handlers.main.logic.MobileAddPushLogic;
import cc.blynk.server.application.handlers.main.logic.MobileLogoutLogic;
import cc.blynk.server.application.handlers.main.logic.MobileSyncLogic;
import cc.blynk.server.application.handlers.main.logic.dashboard.device.MobileGetDevicesLogic;
import cc.blynk.server.application.handlers.main.logic.graph.MobileDeleteDeviceDataLogic;
import cc.blynk.server.application.handlers.main.logic.graph.MobileGetEnhancedGraphDataLogic;
import cc.blynk.server.application.handlers.sharing.auth.MobileShareStateHolder;
import cc.blynk.server.application.handlers.sharing.logic.MobileShareHardwareLogic;
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
public class MobileShareHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final MobileShareStateHolder state;
    private final Holder holder;
    private final MobileShareHardwareLogic hardwareApp;
    private final MobileAddPushLogic mobileAddPushLogic;

    public MobileShareHandler(Holder holder, MobileShareStateHolder state) {
        super(StringMessage.class);
        this.state = state;
        this.holder = holder;

        this.hardwareApp = new MobileShareHardwareLogic(holder, state.userKey.email);
        this.mobileAddPushLogic = new MobileAddPushLogic(holder);
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
                mobileAddPushLogic.messageReceived(ctx, state, msg);
                break;
            case GET_ENHANCED_GRAPH_DATA :
                MobileGetEnhancedGraphDataLogic.messageReceived(holder, ctx, state, msg);
                break;
            case GET_DEVICES :
                MobileGetDevicesLogic.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
            case APP_SYNC :
                MobileSyncLogic.messageReceived(ctx, state, msg);
                break;
            case LOGOUT :
                MobileLogoutLogic.messageReceived(ctx, state.user, msg);
                break;
            case DELETE_DEVICE_DATA :
                MobileDeleteDeviceDataLogic.messageReceived(holder, ctx, state, msg);
                break;
        }
    }

    @Override
    public StateHolderBase getState() {
        return state;
    }
}

package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.AppSyncWidget;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2Device;

/**
 * Request state sync info for widgets.
 * Supports sync for all widgets and sync for specific target
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class AppSyncLogic {

    private AppSyncLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String[] dashIdAndTargetIdString = split2Device(message.body);
        int dashId = Integer.parseInt(dashIdAndTargetIdString[0]);
        int targetId = AppSyncWidget.ANY_TARGET;

        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        if (dashIdAndTargetIdString.length == 2) {
            targetId = Integer.parseInt(dashIdAndTargetIdString[1]);
        }

        ctx.write(ok(message.id), ctx.voidPromise());
        dash.sendSyncs(ctx.channel(), targetId);
        ctx.flush();
    }

}

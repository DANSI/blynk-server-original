package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.widgets.AppSyncWidget;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.AppStateHolderUtil.getAppState;
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
        var dashIdAndTargetIdString = split2Device(message.body);
        var dashId = Integer.parseInt(dashIdAndTargetIdString[0]);
        var targetId = AppSyncWidget.ANY_TARGET;

        var dash = state.user.profile.getDashByIdOrThrow(dashId);

        if (dashIdAndTargetIdString.length == 2) {
            targetId = Integer.parseInt(dashIdAndTargetIdString[1]);
        }

        ctx.write(ok(message.id), ctx.voidPromise());
        Channel appChannel = ctx.channel();
        AppStateHolder appStateHolder = getAppState(appChannel);
        boolean isNewSyncFormat = appStateHolder != null && appStateHolder.isNewSyncFormat();
        dash.sendAppSyncs(appChannel, targetId, isNewSyncFormat);
        ctx.flush();
    }

}

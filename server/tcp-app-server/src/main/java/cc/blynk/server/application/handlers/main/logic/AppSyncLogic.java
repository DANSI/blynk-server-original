package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.AppSyncWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.internal.BlynkByteBufUtil.ok;
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
        int dashId = ParseUtil.parseInt(dashIdAndTargetIdString[0]);
        int targetId = AppSyncWidget.ANY_TARGET;

        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        if (dashIdAndTargetIdString.length == 2) {
            targetId = ParseUtil.parseInt(dashIdAndTargetIdString[1]);

            //special case. app sync with targetId most probably comes from DeviceTiles widget
            //so we do update for it.
            //we actually don't care what DeviceTiles is opened on UI, just update all
            for (Widget widget : dash.widgets) {
                if (widget instanceof DeviceTiles) {
                    ((DeviceTiles) widget).selectedDeviceId = targetId;
                }
            }
        }

        ctx.write(ok(message.id), ctx.voidPromise());
        dash.sendSyncs(ctx.channel(), targetId);
        ctx.flush();
    }

}

package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.AppSyncWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTile;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.model.widgets.AppSyncWidget.SYNC_DEFAULT_MESSAGE_ID;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.BlynkByteBufUtil.ok;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_WIDGETS;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;
import static cc.blynk.utils.StringUtils.split3Device;

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
        String[] dashIdAndTargetIdString = split3Device(message.body);
        int dashId = ParseUtil.parseInt(dashIdAndTargetIdString[0]);
        int targetId = AppSyncWidget.ANY_TARGET;
        long widgetId = -1;

        if (dashIdAndTargetIdString.length == 2) {
            targetId = ParseUtil.parseInt(dashIdAndTargetIdString[1]);
        }

        if (dashIdAndTargetIdString.length == 3) {
            targetId = ParseUtil.parseInt(dashIdAndTargetIdString[1]);
            widgetId = ParseUtil.parseLong(dashIdAndTargetIdString[2]);
        }

        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        ctx.write(ok(message.id), ctx.voidPromise());

        if (widgetId == -1) {
            dash.sendSyncs(ctx.channel(), targetId);
        } else {
            Widget widget = dash.getWidgetByIdOrThrow(widgetId);
            if (widget instanceof DeviceTiles) {
                DeviceTiles deviceTiles = (DeviceTiles) widget;
                DashBoard.sendSyncs(ctx.channel(), targetId, dashId, EMPTY_WIDGETS, dash.pinsStorage);
                for (DeviceTile tile : deviceTiles.tiles) {
                    if (tile.deviceId == targetId && tile.dataStream != null) {
                        String hardBody = tile.dataStream.makeHardwareBody();
                        if (hardBody != null) {
                            String body = prependDashIdAndDeviceId(dashId, targetId, hardBody);
                            ctx.channel().write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body));
                        }
                    }
                }
            }
        }

        ctx.flush();
    }

}

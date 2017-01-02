package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.AppSyncWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

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
public class AppSyncLogic {

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String[] dashIdAndTargetIdString = split2Device(message.body);
        int dashId = ParseUtil.parseInt(dashIdAndTargetIdString[0]);
        int targetId = AppSyncWidget.ANY_TARGET;

        if (dashIdAndTargetIdString.length == 2) {
            targetId = ParseUtil.parseInt(dashIdAndTargetIdString[1]);
        }

        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        final Channel appChannel = ctx.channel();
        for (Widget widget : dash.widgets) {
            if (widget instanceof AppSyncWidget) {
                ((AppSyncWidget) widget).sendAppSync(appChannel, dash.id, targetId);
            }
        }

        ctx.flush();
    }

}

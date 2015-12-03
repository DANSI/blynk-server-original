package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.widgets.Widget;
import cc.blynk.server.model.widgets.controls.SyncWidget;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareSyncLogic {

    public HardwareSyncLogic() {

    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        final int dashId = state.dashId;
        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        for (Widget widget : dash.widgets) {
            if (widget instanceof SyncWidget) {
                ((SyncWidget) widget).send(ctx, message.id);
            }
        }

        ctx.flush();
    }

}

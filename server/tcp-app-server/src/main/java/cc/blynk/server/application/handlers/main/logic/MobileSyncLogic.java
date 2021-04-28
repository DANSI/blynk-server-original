package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.MobileSyncWidget;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.Channel;
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
public final class MobileSyncLogic {

    private MobileSyncLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state, StringMessage message) {
        String[] dashIdAndTargetIdString = split2Device(message.body);
        int dashId = Integer.parseInt(dashIdAndTargetIdString[0]);
        int targetId = MobileSyncWidget.ANY_TARGET;

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        if (dashIdAndTargetIdString.length == 2) {
            targetId = Integer.parseInt(dashIdAndTargetIdString[1]);
        }

        ctx.write(ok(message.id), ctx.voidPromise());
        Channel appChannel = ctx.channel();
        user.profile.sendAppSyncs(dash, appChannel, targetId);
        ctx.flush();
    }

}

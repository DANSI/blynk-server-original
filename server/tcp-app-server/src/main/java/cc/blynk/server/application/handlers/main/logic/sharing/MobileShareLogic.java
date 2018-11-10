package cc.blynk.server.application.handlers.main.logic.sharing;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class MobileShareLogic {

    private MobileShareLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        String[] splitted = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        int dashId = Integer.parseInt(splitted[0]);
        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        switch (splitted[1]) {
            case "on" :
                dash.isShared = true;
                break;
            default :
                dash.isShared = false;
                break;
        }

        Session session = holder.sessionDao.get(state.userKey);
        session.sendToSharedApps(ctx.channel(), dash.sharedToken, message.command, message.id, message.body);
        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }
}

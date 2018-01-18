package cc.blynk.server.application.handlers.main.logic.sharing;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class ShareLogic {

    private final SessionDao sessionDao;

    public ShareLogic(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String[] splitted = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        int dashId = ParseUtil.parseInt(splitted[0]);
        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        switch (splitted[1]) {
            case "on" :
                dash.isShared = true;
                break;
            case "off" :
                dash.isShared = false;
                break;
            default:
                throw new IllegalCommandBodyException("Wrong share command body");
        }

        Session session = sessionDao.userSession.get(state.userKey);
        session.sendToSharedApps(ctx.channel(), dash.sharedToken, message.command, message.id, message.body);
        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }
}

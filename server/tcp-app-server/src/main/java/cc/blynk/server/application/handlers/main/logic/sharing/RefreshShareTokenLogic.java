package cc.blynk.server.application.handlers.main.logic.sharing;

import cc.blynk.server.application.handlers.sharing.auth.AppShareStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.AppStateHolderUtil.*;
import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class RefreshShareTokenLogic {

    private final UserDao userDao;
    private final SessionDao sessionDao;

    public RefreshShareTokenLogic(UserDao userDao, SessionDao sessionDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId;
        try {
            dashId = Integer.parseInt(dashBoardIdString);
        } catch (NumberFormatException ex) {
            throw new NotAllowedException(String.format("Dash board id '%s' not valid.", dashBoardIdString), message.id);
        }

        user.profile.validateDashId(dashId, message.id);

        String token = userDao.sharedTokenManager.refreshToken(user, dashId, user.dashShareTokens);

        Session session = sessionDao.userSession.get(user);
        for (Channel appChannel : session.getAppChannels()) {
            AppShareStateHolder state = getShareState(appChannel);
            if (state != null && state.dashId == dashId) {
                ChannelFuture cf = appChannel.writeAndFlush(makeResponse(message.id, NOT_ALLOWED));
                cf.addListener(channelFuture -> appChannel.close());
            }
        }

        ctx.writeAndFlush(makeStringMessage(REFRESH_SHARE_TOKEN, message.id, token), ctx.voidPromise());
    }
}

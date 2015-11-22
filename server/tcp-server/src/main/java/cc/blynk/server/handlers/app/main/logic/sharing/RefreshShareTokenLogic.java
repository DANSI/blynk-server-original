package cc.blynk.server.handlers.app.main.logic.sharing;

import cc.blynk.common.enums.Command;
import cc.blynk.common.enums.Response;
import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.appllication.sharing.RefreshShareTokenMessage;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.app.sharing.auth.AppShareStateHolder;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.utils.StateHolderUtil.*;

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
        for (Channel appChannel : session.appChannels) {
            AppShareStateHolder state = getShareState(appChannel);
            if (state != null && state.dashId == dashId) {
                ChannelFuture cf = appChannel.writeAndFlush(new ResponseMessage(message.id, Command.RESPONSE, Response.NOT_ALLOWED));
                cf.addListener(channelFuture -> appChannel.close());
            }
        }

        ctx.writeAndFlush(new RefreshShareTokenMessage(message.id, token));
    }
}

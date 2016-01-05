package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.appllication.RefreshTokenMessage;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.utils.ParseUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class RefreshTokenLogic {

    private final UserDao userDao;

    public RefreshTokenLogic(UserDao userDao) {
        this.userDao = userDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId = parseInt(dashBoardIdString, message.id);

        user.profile.validateDashId(dashId, message.id);

        String token = userDao.tokenManager.refreshToken(user, dashId, user.dashTokens);

        ctx.writeAndFlush(new RefreshTokenMessage(message.id, token));
    }
}

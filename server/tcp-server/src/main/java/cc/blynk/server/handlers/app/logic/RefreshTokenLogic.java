package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.model.messages.MessageFactory.produce;

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

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        String dashBoardIdString = message.body;

        int dashId = ParseUtil.parseInt(dashBoardIdString, message.id);

        user.profile.validateDashId(dashId, message.id);

        String token = userDao.tokenManager.refreshToken(user, dashId, user.dashTokens);

        ctx.writeAndFlush(produce(message.id, message.command, token));
    }
}

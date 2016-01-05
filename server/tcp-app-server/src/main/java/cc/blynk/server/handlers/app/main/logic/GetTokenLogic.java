package cc.blynk.server.handlers.app.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.appllication.GetTokenMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetTokenLogic {

    private final UserDao userDao;

    public GetTokenLogic( UserDao userDao) {
        this.userDao = userDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId = ParseUtil.parseInt(dashBoardIdString, message.id);

        user.profile.validateDashId(dashId, message.id);

        String token = userDao.tokenManager.getToken(user, dashId);

        ctx.writeAndFlush(new GetTokenMessage(message.id, token));
    }
}

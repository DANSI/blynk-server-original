package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.utils.ByteBufUtil.*;

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

        ctx.writeAndFlush(makeStringMessage(ctx, GET_TOKEN, message.id, token), ctx.voidPromise());
    }
}

package cc.blynk.server.application.handlers.main.logic.sharing;

import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetShareTokenLogic {

    private static final int PRIVATE_TOKEN_PRICE = 1000;

    private final UserDao userDao;

    public GetShareTokenLogic(UserDao userDao) {
        this.userDao = userDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId;
        try {
            dashId = Integer.parseInt(dashBoardIdString);
        } catch (NumberFormatException ex) {
            throw new NotAllowedException(String.format("Dash board id '%s' not valid.", dashBoardIdString));
        }

        user.profile.validateDashId(dashId);

        Map<Integer, String> tokens = userDao.sharedTokenManager.getTokens(user);
        boolean newToken = tokens.get(dashId) == null;

        String token = userDao.sharedTokenManager.getToken(user, dashId);

        if (newToken) {
            user.subtractEnergy(PRIVATE_TOKEN_PRICE);
        }

        ctx.writeAndFlush(makeStringMessage(GET_SHARE_TOKEN, message.id, token), ctx.voidPromise());
    }
}

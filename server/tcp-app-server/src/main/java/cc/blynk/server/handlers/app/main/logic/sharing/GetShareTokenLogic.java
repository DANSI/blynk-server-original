package cc.blynk.server.handlers.app.main.logic.sharing;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.appllication.sharing.GetShareTokenMessage;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.exceptions.NotAllowedException;
import cc.blynk.server.core.model.auth.User;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetShareTokenLogic {

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
            throw new NotAllowedException(String.format("Dash board id '%s' not valid.", dashBoardIdString), message.id);
        }

        user.profile.validateDashId(dashId, message.id);

        String token = userDao.sharedTokenManager.getToken(user, dashId);

        ctx.writeAndFlush(new GetShareTokenMessage(message.id, token));
    }
}

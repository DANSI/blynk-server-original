package cc.blynk.server.handlers.app.logic.sharing;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.model.messages.MessageFactory.produce;

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

        int dashBoardId;
        try {
            dashBoardId = Integer.parseInt(dashBoardIdString);
        } catch (NumberFormatException ex) {
            throw new NotAllowedException(String.format("Dash board id '%s' not valid.", dashBoardIdString), message.id);
        }

        user.profile.validateDashId(dashBoardId, message.id);

        String token = userDao.sharedTokenManager.getToken(user, dashBoardId);

        ctx.writeAndFlush(produce(message.id, message.command, token));
    }
}

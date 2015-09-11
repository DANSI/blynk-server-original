package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.server.dao.UserRegistry;
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
public class RefreshShareTokenLogic {

    private final UserRegistry userRegistry;

    public RefreshShareTokenLogic(UserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        String dashBoardIdString = message.body;

        int dashBoardId;
        try {
            dashBoardId = Integer.parseInt(dashBoardIdString);
        } catch (NumberFormatException ex) {
            throw new NotAllowedException(String.format("Dash board id '%s' not valid.", dashBoardIdString), message.id);
        }

        user.getProfile().validateDashId(dashBoardId, message.id);

        String token = userRegistry.refreshToken(user, dashBoardId, user.getDashShareTokens());

        ctx.writeAndFlush(produce(message.id, message.command, token));
    }
}

package cc.blynk.server.application.handlers.main.logic.sharing;

import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.GET_SHARE_TOKEN;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetShareTokenLogic {

    private static final int PRIVATE_TOKEN_PRICE = 1000;

    private final TokenManager tokenManager;

    public GetShareTokenLogic(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId;
        try {
            dashId = Integer.parseInt(dashBoardIdString);
        } catch (NumberFormatException ex) {
            throw new NotAllowedException("Dash board id not valid. Id : " + dashBoardIdString);
        }

        user.profile.validateDashId(dashId);

        boolean newToken = user.dashShareTokens.get(dashId) == null;

        String token = tokenManager.getSharedToken(user, dashId);

        if (newToken) {
            user.subtractEnergy(PRIVATE_TOKEN_PRICE);
        }

        ctx.writeAndFlush(makeStringMessage(GET_SHARE_TOKEN, message.id, token), ctx.voidPromise());
    }
}

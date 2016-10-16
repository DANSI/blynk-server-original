package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.redis.RedisClient;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.GET_TOKEN;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetTokenLogic {

    private final TokenManager tokenManager;
    private final BlockingIOProcessor blockingIOProcessor;
    private final RedisClient redisClient;
    private final String currentIp;

    public GetTokenLogic(Holder holder) {
        this.tokenManager = holder.tokenManager;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.redisClient = holder.redisClient;
        this.currentIp = holder.currentIp;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashBoardIdString = message.body;

        int dashId = ParseUtil.parseInt(dashBoardIdString);

        user.profile.validateDashId(dashId);

        String token = user.dashTokens.get(dashId);

        //if token not exists. generate new one
        if (token == null) {
            token = tokenManager.refreshToken(user, dashId);
            final String newToken = token;
            blockingIOProcessor.execute(() -> {
                redisClient.assignServerToToken(newToken, currentIp);
            });
        }

        ctx.writeAndFlush(makeStringMessage(GET_TOKEN, message.id, token), ctx.voidPromise());
    }
}

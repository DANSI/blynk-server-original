package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import cc.blynk.server.redis.RedisClient;
import cc.blynk.utils.LoadBalancerUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.validator.routines.EmailValidator;

import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND_BODY;
import static cc.blynk.utils.ByteBufUtil.makeResponse;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.10.16.
 */
@ChannelHandler.Sharable
public class GetServerHandler extends SimpleChannelInboundHandler<GetServerMessage> {

    private final String[] loadBalancingIps;
    private final BlockingIOProcessor blockingIOProcessor;
    private final RedisClient redisClient;

    public GetServerHandler(Holder holder, String[] ips) {
        super();
        this.loadBalancingIps = ips;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.redisClient = holder.redisClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GetServerMessage msg) throws Exception {
        final String username = msg.body;

        if (username == null || username.equals("")) {
            ctx.writeAndFlush(makeResponse(msg.id, Response.ILLEGAL_COMMAND));
            return;
        }

        if (username.length() > 255 || !EmailValidator.getInstance().isValid(username)) {
            ctx.writeAndFlush(makeResponse(msg.id, ILLEGAL_COMMAND_BODY), ctx.voidPromise());
            return;
        }

        String server = LoadBalancerUtil.getSuitableServer(loadBalancingIps, username);
        blockingIOProcessor.execute(() -> {
            redisClient.assignServerToUser(username, server);
        });
        ctx.writeAndFlush(makeStringMessage(msg.command, msg.id, server));
    }

}

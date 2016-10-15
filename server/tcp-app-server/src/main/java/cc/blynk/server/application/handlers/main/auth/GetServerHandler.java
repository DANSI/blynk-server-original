package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static cc.blynk.utils.ByteBufUtil.makeResponse;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.10.16.
 */
@ChannelHandler.Sharable
public class GetServerHandler extends SimpleChannelInboundHandler<GetServerMessage> {

    private final String[] ips;


    public GetServerHandler(String[] ips) {
        super();
        this.ips = ips;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GetServerMessage msg) throws Exception {
        if (msg.body == null || msg.body.equals("")) {
            ctx.writeAndFlush(makeResponse(msg.id, Response.ILLEGAL_COMMAND));
            return;
        }
        String server = getSuitableServer(msg.body);
        ctx.writeAndFlush(makeStringMessage(msg.command, msg.id, server));


    }

    //for now support only 1 ip
    private String getSuitableServer(String username) {
        return ips[0];
    }
}

package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;
import java.util.Map;

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
    private final Map<String, String> distributedStorage;

    public GetServerHandler(String[] ips) {
        super();
        this.ips = ips;
        this.distributedStorage = new HashMap<>();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GetServerMessage msg) throws Exception {
        final String server = getServer(msg.body);
        if (server == null) {
            ctx.writeAndFlush(makeResponse(msg.id, Response.ILLEGAL_COMMAND));
            return;
        }
        ctx.writeAndFlush(makeStringMessage(msg.command, msg.id, server));
    }

    private String getServer(String username) {
        if (username == null || username.equals("")) {
            return null;
        }
        String server = distributedStorage.get(username);

        if (server != null) {
            return server;
        }

        //this is possible in case user is new or value not updated yet from other server
        //for now second case considered as unreal.
        return getSuitableServer(username);
    }

    //for now support only 1 ip
    private String getSuitableServer(String username) {
        return ips[0];
    }
}

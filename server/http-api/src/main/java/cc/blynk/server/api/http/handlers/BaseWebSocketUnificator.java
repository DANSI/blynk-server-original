package cc.blynk.server.api.http.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.02.18.
 */
@ChannelHandler.Sharable
public abstract class BaseWebSocketUnificator extends ChannelInboundHandlerAdapter {

    @Override
    public abstract void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

}

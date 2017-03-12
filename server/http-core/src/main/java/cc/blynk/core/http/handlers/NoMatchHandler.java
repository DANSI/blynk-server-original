package cc.blynk.core.http.handlers;

import cc.blynk.core.http.Response;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.03.17.
 */
@ChannelHandler.Sharable
public class NoMatchHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(NoMatchHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if ((msg instanceof HttpRequest)) {
            HttpRequest req = (HttpRequest) msg;
            log.debug("Error resolving url. No path found. {} : {}", req.method().name(), req.uri());
            ctx.writeAndFlush(Response.notFound(), ctx.voidPromise());
        }
    }
}

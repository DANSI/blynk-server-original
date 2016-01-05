package cc.blynk.server.core;

import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public class BaseHttpHandler extends ChannelInboundHandlerAdapter {

    protected static final Logger log = LogManager.getLogger(BaseHttpHandler.class);

    protected static void send(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse response) {
        if (!HttpHeaders.isKeepAlive(req)) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            ctx.write(response);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpRequest)) {
            return;
        }

        HttpRequest req = (HttpRequest) msg;

        log.info("{} : {}", req.getMethod().name(), req.getUri());

        process(ctx, req);
    }

    public void process(ChannelHandlerContext ctx, HttpRequest request) {
        FullHttpResponse response = HandlerRegistry.process(request);
        send(ctx, request, response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error in http handler.", cause);
        ctx.close();
    }

}

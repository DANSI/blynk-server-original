package cc.blynk.server.handlers.http.admin;

import cc.blynk.server.handlers.http.admin.handlers.FileHandler;
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
 * Created on 01.12.15.
 */
public class HttpAdminHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(HttpAdminHandler.class);

    private final FileHandler fileHandler = new FileHandler();

    private static void send(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse response) {
        if (!HttpHeaders.isKeepAlive(req)) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            ctx.write(response);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpRequest)) {
            return;
        }

        HttpRequest req = (HttpRequest) msg;

        log.info("URL : {}", req.getUri());

        //a bit ugly code but it is ok for now. 2 branches. 1 fro static files, second for normal http api
        if (req.getUri().startsWith("/admin/static")) {
            try {
                fileHandler.channelRead(ctx, req);
            } catch (Exception e) {
                log.error("Error handling static file.", e);
            }
        } else {
            FullHttpResponse response = HandlerRegistry.process(req);
            send(ctx, req, response);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("aaa", cause);
        ctx.close();
    }
}

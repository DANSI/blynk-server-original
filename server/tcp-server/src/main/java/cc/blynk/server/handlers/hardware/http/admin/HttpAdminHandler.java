package cc.blynk.server.handlers.hardware.http.admin;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.hardware.http.URIDecoder;
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

    private final HttpAdminRequestHandler httpAdminRequestHandler;
    private final UserDao userDao;

    public HttpAdminHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        this.userDao = userDao;
        this.httpAdminRequestHandler = new HttpAdminRequestHandler(userDao, sessionDao, globalStats);
    }

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

        URIDecoder uriDecoder = new URIDecoder(req.getUri());

        FullHttpResponse response;
        switch (uriDecoder.getHandlerType()) {
            case "admin" :
                response = httpAdminRequestHandler.processRequest(req, uriDecoder);
                break;
            default :
                response = null;
        }

        send(ctx, req, response);

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

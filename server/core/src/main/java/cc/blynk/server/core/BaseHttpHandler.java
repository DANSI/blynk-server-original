package cc.blynk.server.core;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.server.handlers.http.rest.HandlerHolder;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import cc.blynk.server.handlers.http.rest.Response;
import cc.blynk.server.handlers.http.rest.URIDecoder;
import io.netty.channel.Channel;
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
public class BaseHttpHandler extends ChannelInboundHandlerAdapter implements DefaultReregisterHandler {

    protected static final Logger log = LogManager.getLogger(BaseHttpHandler.class);

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final GlobalStats globalStats;

    public BaseHttpHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.globalStats = globalStats;
    }

    private static void send(ChannelHandlerContext ctx, FullHttpResponse response) {
        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        ctx.writeAndFlush(response);
    }

    private static void send(Channel channel, FullHttpResponse response) {
        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        channel.writeAndFlush(response);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpRequest)) {
            return;
        }

        HttpRequest req = (HttpRequest) msg;

        log.info("{} : {}", req.getMethod().name(), req.getUri());

        globalStats.mark(Command.HTTP_TOTAL);
        processHttp(ctx, req);
    }

    public void processHttp(ChannelHandlerContext ctx, HttpRequest req) {
        HandlerHolder handlerHolder = HandlerRegistry.findHandler(req.getMethod(), HandlerRegistry.path(req.getUri()));

        if (handlerHolder == null) {
            log.error("Error resolving url. No path found.");
            send(ctx, Response.notFound());
        } else {
            URIDecoder uriDecoder = new URIDecoder(req.getUri());
            HandlerRegistry.populateBody(req, uriDecoder);
            uriDecoder.pathData = handlerHolder.uriTemplate.extractParameters();

            //reregister logic
            String tokenPathParam = uriDecoder.pathData.get("token");
            if (tokenPathParam != null) {
                User user = userDao.tokenManager.getUserByToken(tokenPathParam);
                if (user != null) {
                    Session session = sessionDao.getSessionByUser(user, ctx.channel().eventLoop());
                    if (session.initialEventLoop != ctx.channel().eventLoop()) {
                        log.debug("Re registering http channel. {}", ctx.channel());
                        reRegisterChannel(ctx, session, channelFuture -> send(channelFuture.channel(), HandlerRegistry.invoke(handlerHolder, uriDecoder)));
                    } else {
                        send(ctx, HandlerRegistry.invoke(handlerHolder, uriDecoder));
                    }
                } else {
                    log.error("Requested token {} not found.", tokenPathParam);
                    send(ctx, Response.badRequest("Invalid token."));
                }
            } else {
                send(ctx, HandlerRegistry.invoke(handlerHolder, uriDecoder));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error in http handler.", cause);
        ctx.close();
    }

}

package cc.blynk.core.http;

import cc.blynk.core.http.rest.Handler;
import cc.blynk.core.http.rest.HandlerHolder;
import cc.blynk.core.http.rest.URIDecoder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.DefaultReregisterHandler;
import cc.blynk.utils.AnnotationsUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public abstract class BaseHttpHandler extends ChannelInboundHandlerAdapter implements DefaultReregisterHandler, DefaultExceptionHandler {

    protected static final Logger log = LogManager.getLogger(BaseHttpHandler.class);

    protected final TokenManager tokenManager;
    protected final SessionDao sessionDao;
    protected final GlobalStats globalStats;
    protected final Handler[] handlers;

    public BaseHttpHandler(TokenManager tokenManager, SessionDao sessionDao, GlobalStats globalStats) {
        this.tokenManager = tokenManager;
        this.sessionDao = sessionDao;
        this.globalStats = globalStats;
        this.handlers = AnnotationsUtil.register(this);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            HandlerHolder handlerHolder = lookupHandler(req);

            if (handlerHolder != null) {
                log.debug("{} : {}", req.method().name(), req.uri());
                globalStats.mark(Command.HTTP_TOTAL);
                processHttp(ctx, req, handlerHolder);
            } else {
                ctx.fireChannelRead(msg);
            }
        }
    }

    public void processHttp(ChannelHandlerContext ctx, HttpRequest req, HandlerHolder handlerHolder) {
        URIDecoder uriDecoder;
        Object[] params;

        try {
            uriDecoder = new URIDecoder(req);
            uriDecoder.pathData = handlerHolder.extractParameters();
            params = handlerHolder.handler.fetchParams(uriDecoder);
        } catch (StringIndexOutOfBoundsException stringE) {
            log.error("{} : '{}'. Error : ", req.method().name(), req.uri(), stringE.getMessage());
            ctx.writeAndFlush(Response.serverError(stringE.getMessage()), ctx.voidPromise());
            return;
        } catch (Exception e) {
            ctx.writeAndFlush(Response.serverError(e.getMessage()), ctx.voidPromise());
            return;
        } finally {
            ReferenceCountUtil.release(req);
        }

        finishHttp(ctx, uriDecoder, handlerHolder.handler, params);
    }

    public void finishHttp(ChannelHandlerContext ctx, URIDecoder uriDecoder, Handler handlerHolder, Object[] params) {
        String tokenPathParam = uriDecoder.pathData.get("token");
        if (tokenPathParam == null) {
            ctx.writeAndFlush(AnnotationsUtil.invoke(handlerHolder, params), ctx.voidPromise());
            return;
        }

        //reregister logic
        TokenValue tokenValue = tokenManager.getUserByToken(tokenPathParam);
        if (tokenValue == null) {
            log.warn("Requested token {} not found.", tokenPathParam);
            ctx.writeAndFlush(Response.badRequest("Invalid token."), ctx.voidPromise());
            return;
        }

        Session session = sessionDao.getOrCreateSessionByUser(new UserKey(tokenValue.user), ctx.channel().eventLoop());
        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering http channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), AnnotationsUtil.invoke(handlerHolder, params)));
        } else {
            completeLogin(ctx.channel(), AnnotationsUtil.invoke(handlerHolder, params));
        }
    }

    private HandlerHolder lookupHandler(HttpRequest req) {
        for (Handler handler : handlers) {
            if (handler.httpMethod == req.method()) {
                Matcher matcher = handler.uriTemplate.matcher(req.uri());
                if (matcher.matches()) {
                    return new HandlerHolder(handler, matcher);
                }
            }
        }
        return null;
    }

    private static String path(String uri) {
        int pathEndPos = uri.indexOf('?');
        if (pathEndPos < 0) {
            return uri;
        } else {
            return uri.substring(0, pathEndPos);
        }
    }

    private void completeLogin(Channel channel, FullHttpResponse response) {
        channel.writeAndFlush(response);
        log.debug("Re registering http channel finished.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleUnexpectedException(ctx, cause);
    }

}

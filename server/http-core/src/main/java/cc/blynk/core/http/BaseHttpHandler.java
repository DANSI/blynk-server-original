package cc.blynk.core.http;

import cc.blynk.core.http.rest.HandlerHolder;
import cc.blynk.core.http.rest.HandlerWrapper;
import cc.blynk.core.http.rest.URIDecoder;
import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.regex.Matcher;

import static cc.blynk.core.http.Response.serverError;
import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleUnexpectedException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public abstract class BaseHttpHandler extends ChannelInboundHandlerAdapter {

    protected static final Logger log = LogManager.getLogger(BaseHttpHandler.class);

    protected final TokenManager tokenManager;
    protected final SessionDao sessionDao;
    protected final HandlerWrapper[] handlers;
    protected final String rootPath;

    public BaseHttpHandler(Holder holder, String rootPath) {
        this(holder.tokenManager, holder.sessionDao, holder.stats, rootPath);
    }

    BaseHttpHandler(TokenManager tokenManager, SessionDao sessionDao,
                           GlobalStats globalStats, String rootPath) {
        this.tokenManager = tokenManager;
        this.sessionDao = sessionDao;
        this.rootPath = rootPath;
        this.handlers = AnnotationsProcessor.register(rootPath, this, globalStats);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            if (!process(ctx, req)) {
                ctx.fireChannelRead(req);
            }
        }
    }

    public boolean process(ChannelHandlerContext ctx, HttpRequest req) {
        HandlerHolder handlerHolder = lookupHandler(req);

        if (handlerHolder != null) {
            try {
                invokeHandler(ctx, req, handlerHolder.handler, handlerHolder.extractedParams);
            } catch (Exception e) {
                log.debug("Error processing http request.", e);
                ctx.writeAndFlush(serverError(e.getMessage()), ctx.voidPromise());
            } finally {
                ReferenceCountUtil.release(req);
            }
            return true;
        }

        return false;
    }

    private void invokeHandler(ChannelHandlerContext ctx, HttpRequest req,
                               HandlerWrapper handler, Map<String, String> extractedParams) {
        log.debug("{} : {}", req.method().name(), req.uri());
        try (URIDecoder uriDecoder = new URIDecoder(req, extractedParams)) {
            Object[] params = handler.fetchParams(ctx, uriDecoder);
            finishHttp(ctx, uriDecoder, handler, params);
        }
    }

    public void finishHttp(ChannelHandlerContext ctx, URIDecoder uriDecoder,
                           HandlerWrapper handler, Object[] params) {
        FullHttpResponse response = handler.invoke(params);
        if (response != Response.NO_RESPONSE) {
            ctx.writeAndFlush(response);
        }
    }

    private HandlerHolder lookupHandler(HttpRequest req) {
        for (HandlerWrapper handler : handlers) {
            if (handler.httpMethod == req.method()) {
                Matcher matcher = handler.uriTemplate.matcher(req.uri());
                if (matcher.matches()) {
                    Map<String, String> extractedParams = handler.uriTemplate.extractParameters(matcher);
                    return new HandlerHolder(handler, extractedParams);
                }
            }
        }
        return null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleUnexpectedException(ctx, cause);
    }

}

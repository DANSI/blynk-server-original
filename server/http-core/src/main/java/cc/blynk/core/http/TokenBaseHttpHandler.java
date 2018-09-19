package cc.blynk.core.http;

import cc.blynk.core.http.rest.HandlerWrapper;
import cc.blynk.core.http.rest.URIDecoder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.internal.ReregisterChannelUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public abstract class TokenBaseHttpHandler extends BaseHttpHandler {

    public TokenBaseHttpHandler(TokenManager tokenManager, SessionDao sessionDao,
                                GlobalStats globalStats, String rootPath) {
        super(tokenManager, sessionDao, globalStats, rootPath);
    }

    @Override
    public void finishHttp(ChannelHandlerContext ctx, URIDecoder uriDecoder,
                           HandlerWrapper handler, Object[] params) {
        String tokenPathParam = uriDecoder.pathData.get("token");
        if (tokenPathParam == null) {
            ctx.writeAndFlush(Response.badRequest("No token provided."));
            return;
        }

        //reregister logic
        TokenValue tokenValue = tokenManager.getTokenValueByToken(tokenPathParam);
        if (tokenValue == null) {
            log.debug("Requested token {} not found.", tokenPathParam);
            ctx.writeAndFlush(Response.badRequest("Invalid token."), ctx.voidPromise());
            return;
        }

        Session session = sessionDao.getOrCreateSessionByUser(new UserKey(tokenValue.user), ctx.channel().eventLoop());
        if (session.isSameEventLoop(ctx)) {
            completeLogin(ctx.channel(), handler.invoke(params));
        } else {
            log.trace("Re registering http channel. {}", ctx.channel());
            ReregisterChannelUtil.reRegisterChannel(ctx, session, channelFuture -> completeLogin(
                    channelFuture.channel(), handler.invoke(params)));
        }
    }

    private void completeLogin(Channel channel, FullHttpResponse response) {
        channel.writeAndFlush(response);
        log.trace("Re registering http channel finished.");
    }
}

package cc.blynk.core.http;

import cc.blynk.core.http.rest.Handler;
import cc.blynk.core.http.rest.URIDecoder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public abstract class TokenBaseHttpHandler extends BaseHttpHandler {

    public TokenBaseHttpHandler(TokenManager tokenManager, SessionDao sessionDao, GlobalStats globalStats, String rootPath) {
        super(tokenManager, sessionDao, globalStats, rootPath);
    }

    @Override
    public void finishHttp(ChannelHandlerContext ctx, URIDecoder uriDecoder, Handler handler, Object[] params) {
        String tokenPathParam = uriDecoder.pathData.get("token");
        if (tokenPathParam == null) {
            ctx.writeAndFlush(Response.badRequest("No token provided."));
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
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), handler.invoke(params)));
        } else {
            completeLogin(ctx.channel(), handler.invoke(params));
        }
    }

    private void completeLogin(Channel channel, FullHttpResponse response) {
        channel.writeAndFlush(response);
        log.debug("Re registering http channel finished.");
    }
}

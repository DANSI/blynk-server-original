package cc.blynk.server.admin.http;

import cc.blynk.server.admin.http.logic.business.AuthCookieHandler;
import cc.blynk.server.core.BaseHttpHandler;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.http.rest.HandlerHolder;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import cc.blynk.server.handlers.http.rest.URIDecoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.01.16.
 */
public class AuthHttpHandler extends BaseHttpHandler {

    public AuthHttpHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        super(userDao, sessionDao, globalStats);
    }

    @Override
    public void finishHttp(ChannelHandlerContext ctx, URIDecoder uriDecoder, HandlerHolder handlerHolder, Object[] params) {
        User user = ctx.channel().attr(AuthCookieHandler.userAttributeKey).get();

        if (user == null) {
            super.finishHttp(ctx, uriDecoder, handlerHolder, params);
            return;
        }

        params[0] = user;

        Session session = sessionDao.getSessionByUser(user, ctx.channel().eventLoop());
        if (session.initialEventLoop != ctx.channel().eventLoop()) {
            log.debug("Re registering http channel. {}", ctx.channel());
            reRegisterChannel(ctx, session, channelFuture -> completeLogin(channelFuture.channel(), HandlerRegistry.invoke(handlerHolder, params)));
        } else {
            completeLogin(ctx.channel(), HandlerRegistry.invoke(handlerHolder, params));
        }
    }

    private void completeLogin(Channel channel, FullHttpResponse response) {
        channel.writeAndFlush(response);
        log.debug("Re registering http channel finished.");
    }
}

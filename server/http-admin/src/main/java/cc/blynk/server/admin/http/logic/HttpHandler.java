package cc.blynk.server.admin.http.logic;

import cc.blynk.server.core.BaseHttpHandler;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.http.rest.HandlerHolder;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import cc.blynk.server.handlers.http.rest.URIDecoder;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.05.16.
 */
public class HttpHandler extends BaseHttpHandler {

    public HttpHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        super(userDao, sessionDao, globalStats);
    }

    @Override
    public void finishHttp(ChannelHandlerContext ctx, URIDecoder uriDecoder, HandlerHolder handlerHolder, Object[] params) {
        ctx.writeAndFlush(HandlerRegistry.invoke(handlerHolder, params));
    }

}

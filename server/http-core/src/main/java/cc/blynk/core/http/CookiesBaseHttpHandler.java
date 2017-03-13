package cc.blynk.core.http;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public abstract class CookiesBaseHttpHandler extends BaseHttpHandler {

    public CookiesBaseHttpHandler(Holder holder, String rootPath) {
        super(holder, rootPath);
    }

    public CookiesBaseHttpHandler(TokenManager tokenManager, SessionDao sessionDao, GlobalStats globalStats, String rootPath) {
        super(tokenManager, sessionDao, globalStats, rootPath);
    }

    @Override
    public void process(ChannelHandlerContext ctx, HttpRequest req) {
        if (req.headers().contains(COOKIE)) {
            super.process(ctx, req);
        } else {
            ctx.fireChannelRead(req);
        }
    }
}

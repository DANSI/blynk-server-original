package cc.blynk.core.http;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public abstract class CookiesBaseHttpHandler extends BaseHttpHandler {

    public CookiesBaseHttpHandler(Holder holder, String rootPath) {
        super(holder, rootPath);
    }

    public CookiesBaseHttpHandler(TokenManager tokenManager, SessionDao sessionDao,
                                  GlobalStats globalStats, String rootPath) {
        super(tokenManager, sessionDao, globalStats, rootPath);
    }

    @Override
    public boolean process(ChannelHandlerContext ctx, HttpRequest req) {
        if (ctx.channel().attr(SessionDao.userAttributeKey).get() != null) {
            return super.process(ctx, req);
        }
        return false;
    }
}

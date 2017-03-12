package cc.blynk.core.http;

import cc.blynk.core.http.rest.Handler;
import cc.blynk.core.http.rest.URIDecoder;
import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public abstract class AdminBaseHttpHandler extends BaseHttpHandler {

    public AdminBaseHttpHandler(Holder holder) {
        super(holder.tokenManager, holder.sessionDao, holder.stats);
    }

    public AdminBaseHttpHandler(TokenManager tokenManager, SessionDao sessionDao, GlobalStats globalStats) {
        super(tokenManager, sessionDao, globalStats);
    }

    @Override
    public void finishHttp(ChannelHandlerContext ctx, URIDecoder uriDecoder, Handler handler, Object[] params) {
        FullHttpResponse response = handler.invoke(params);

        //API handlers may return forward request. means we need to proceed pipeline furhter.
        if (response instanceof ForwardResponse) {
            String forwardUrl = ((ForwardResponse) response).url;
            uriDecoder.httpRequest.setUri(forwardUrl);
            ctx.fireChannelRead(uriDecoder.httpRequest);
        } else {
            ctx.writeAndFlush(response);
        }
    }

}

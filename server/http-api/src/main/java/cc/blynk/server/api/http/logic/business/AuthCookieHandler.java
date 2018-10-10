package cc.blynk.server.api.http.logic.business;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.05.16.
 */
@ChannelHandler.Sharable
public class AuthCookieHandler extends ChannelInboundHandlerAdapter {

    private final SessionDao sessionDao;

    public AuthCookieHandler(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            if (request.uri().equals("/admin/logout")) {
                ctx.channel().attr(SessionDao.userAttributeKey).set(null);
            } else {
                User user = sessionDao.getUserFromCookie(request);
                ctx.channel().attr(SessionDao.userAttributeKey).set(user);
            }
        }
        super.channelRead(ctx, msg);
    }

}

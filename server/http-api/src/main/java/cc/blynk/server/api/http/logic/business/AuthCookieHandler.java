package cc.blynk.server.api.http.logic.business;

import cc.blynk.server.core.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.AttributeKey;

import java.util.Set;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.05.16.
 */
@ChannelHandler.Sharable
public class AuthCookieHandler extends ChannelInboundHandlerAdapter {

    public final static AttributeKey<User> userAttributeKey = AttributeKey.valueOf("user");
    private final SessionHolder sessionHolder;

    public AuthCookieHandler(SessionHolder sessionHolder) {
        this.sessionHolder = sessionHolder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            User user = getUserFromCookie(request);

            if (request.uri().equals("/admin/logout")) {
                ctx.channel().attr(userAttributeKey).set(null);
            } else {
                ctx.channel().attr(userAttributeKey).set(user);
            }
        }
        super.channelRead(ctx, msg);
    }

    public User getUserFromCookie(FullHttpRequest request) {
        String cookieString = request.headers().get(HttpHeaderNames.COOKIE);

        if (cookieString != null) {
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);
            if (!cookies.isEmpty()) {
                for (Cookie cookie : cookies) {
                    if (sessionHolder.isValid(cookie)) {
                        return sessionHolder.getUser(cookie.value());
                    }
                }
            }
        }

        return null;
    }

}

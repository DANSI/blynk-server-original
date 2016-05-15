package cc.blynk.server.api.http.logic.business;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.handlers.http.rest.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.05.16.
 */
public class AuthCookieHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(AuthCookieHandler.class);
    public final static AttributeKey<User> userAttributeKey = AttributeKey.valueOf("user");

    private final String authPath;
    private final SessionHolder sessionHolder;

    public AuthCookieHandler(String authPath, SessionHolder sessionHolder) {
        this.authPath = authPath;
        this.sessionHolder = sessionHolder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            if (request.getUri().startsWith(authPath)) {
                User user = getUser(request);

                //access to API that requires cookies and no auth cookie
                if (user != null) {
                    if (request.getUri().equals("/business/logout")) {
                        ctx.channel().attr(userAttributeKey).remove();
                    } else {
                        ctx.channel().attr(userAttributeKey).set(user);
                    }
                } else {
                    if (request.getUri().endsWith("/login") || request.getUri().startsWith("/business/static")) {
                    } else {
                        ctx.writeAndFlush(Response.redirect("/business/static/login.html"));
                        return;
                    }
                }
            }
        }

        super.channelRead(ctx, msg);
    }

    private User getUser(FullHttpRequest request) {
        String cookieString = request.headers().get(HttpHeaders.Names.COOKIE);

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

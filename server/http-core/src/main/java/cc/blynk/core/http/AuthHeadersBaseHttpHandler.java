package cc.blynk.core.http;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.utils.SHA256Util;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
public abstract class AuthHeadersBaseHttpHandler extends BaseHttpHandler {

    public static final AttributeKey<User> USER = AttributeKey.newInstance("USER");

    private final UserDao userDao;

    public AuthHeadersBaseHttpHandler(Holder holder, String rootPath) {
        super(holder, rootPath);
        this.userDao = holder.userDao;
    }

    @Override
    public boolean process(ChannelHandlerContext ctx, HttpRequest req) {
        try {
            User superAdmin = validateAuth(userDao, req);
            if (superAdmin != null) {
                ctx.channel().attr(USER).set(superAdmin);
                return super.process(ctx, req);
            }
        } catch (IllegalAccessException e) {
            //return 403 and stop processing.
            ctx.writeAndFlush(Response.forbidden(e.getMessage()));
            return true;
        }

        return false;
    }

    public static User validateAuth(UserDao userDao, HttpRequest req) throws IllegalAccessException {
        String auth = req.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (auth != null) {
            try {
                String encodedAuth = auth.substring("Basic ".length());
                String decoded = new String(java.util.Base64.getDecoder().decode(encodedAuth));
                String[] userAndPass = decoded.split(":");
                String user = userAndPass[0].toLowerCase();
                String pass = userAndPass[1];

                User superUser = userDao.getSuperAdmin();
                String passHash = SHA256Util.makeHash(pass, user);

                log.info("Header auth attempt. User: {}, pass: {}", user, pass);
                if (superUser != null && superUser.email.equals(user) && superUser.pass.equals(passHash)) {
                    return superUser;
                } else {
                    throw new IllegalAccessException("Authentication failed.");
                }
            } catch (IllegalAccessException iae) {
                log.error("Error invoking OTA handler. {}", iae.getMessage());
                throw iae;
            } catch (Exception e) {
                log.error("Error invoking OTA handler.");
            }
        }
        return null;
    }
}

package cc.blynk.server.api.http.logic.business;

import cc.blynk.core.http.AdminBaseHttpHandler;
import cc.blynk.core.http.MediaType;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.FormParam;
import cc.blynk.core.http.annotation.POST;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import static cc.blynk.core.http.Response.redirect;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/admin")
@ChannelHandler.Sharable
public class AdminAuthHandler extends AdminBaseHttpHandler {

    private final UserDao userDao;
    private final SessionHolder sessionHolder;

    public AdminAuthHandler(Holder holder, SessionHolder sessionHolder) {
        super(holder);
        this.userDao = holder.userDao;
        this.sessionHolder = sessionHolder;
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/login")
    public Response login(@FormParam("email") String email,
                          @FormParam("password") String password) {

        if (email == null || password == null) {
            return redirect("/admin");
        }

        User user = userDao.getByName(email, AppName.BLYNK);

        if (user == null || !user.isSuperAdmin) {
            return redirect("/admin");
        }

        if (!password.equals(user.pass)) {
            return redirect("/admin");
        }

        Response response = redirect("/admin");

        Cookie cookie = makeDefaultSessionCookie(sessionHolder.generateNewSession(user), 86400);
        response.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));

        return response;
    }

    @POST
    @Path("/logout")
    public Response logout() {
        Response response = redirect("/admin");
        Cookie cookie = makeDefaultSessionCookie("", 0);
        response.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
        return response;
    }

    private static Cookie makeDefaultSessionCookie(String sessionId, int maxAge) {
        DefaultCookie cookie = new DefaultCookie(Cookies.SESSION_COOKIE, sessionId);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

}

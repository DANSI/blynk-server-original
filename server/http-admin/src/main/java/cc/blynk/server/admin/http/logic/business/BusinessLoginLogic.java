package cc.blynk.server.admin.http.logic.business;

import cc.blynk.server.admin.http.logic.admin.BaseLogic;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.handlers.http.rest.Response;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import static cc.blynk.server.handlers.http.rest.Response.*;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("")
public class BusinessLoginLogic extends BaseLogic {

    private final UserDao userDao;
    private final SessionHolder sessionHolder;
    private final SessionDao sessionDao;
    private final FileManager fileManager;

    public BusinessLoginLogic(UserDao userDao, SessionDao sessionDao, FileManager fileManager) {
        this.userDao = userDao;
        this.fileManager = fileManager;
        this.sessionDao = sessionDao;
        this.sessionHolder = new SessionHolder();
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/login")
    public Response login(@FormParam("email") String email,
                          @FormParam("password") String password) {

        if (email == null || password == null) {
            return redirect("/business");
        }

        User user = userDao.getByName(email);

        if (user == null) {
            return redirect("/business");
        }

        if (!password.equals(user.pass)) {
            return redirect("/business");
        }

        Response response = redirect("/business/static/business.html");

        Cookie cookie = makeDefaultSessionCookie(sessionHolder.generateNewSession(user));
        response.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));

        return response;
    }

    private static Cookie makeDefaultSessionCookie(String sessionId) {
        DefaultCookie cookie = new DefaultCookie("session", sessionId);
        cookie.setMaxAge(86400);
        return cookie;
    }

}

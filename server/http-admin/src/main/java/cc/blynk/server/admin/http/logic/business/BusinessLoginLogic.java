package cc.blynk.server.admin.http.logic.business;

import cc.blynk.server.admin.http.logic.BaseLogic;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.handlers.http.rest.Response;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("")
public class BusinessLoginLogic extends BaseLogic {

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final FileManager fileManager;

    public BusinessLoginLogic(UserDao userDao, SessionDao sessionDao, FileManager fileManager) {
        this.userDao = userDao;
        this.fileManager = fileManager;
        this.sessionDao = sessionDao;
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/login")
    public Response login(@FormParam("email") String email,
                          @FormParam("password") String password) {

        if (email == null || password == null) {
            return Response.redirect("/business");
        }

        User user = userDao.getByName(email);

        if (user == null) {
            return Response.redirect("/business");
        }

        if (!password.equals(user.pass)) {
            return Response.redirect("/business");
        }

        return Response.notFound();
    }

}

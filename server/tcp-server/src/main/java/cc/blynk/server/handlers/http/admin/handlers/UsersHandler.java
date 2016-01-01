package cc.blynk.server.handlers.http.admin.handlers;

import cc.blynk.common.utils.SHA256Util;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.http.helpers.Filter;
import cc.blynk.server.handlers.http.helpers.Response;
import cc.blynk.server.handlers.http.helpers.pojo.UserPassPojo;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.utils.JsonParser;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static cc.blynk.server.handlers.http.helpers.Response.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.12.15.
 */
@Path("/users")
public class UsersHandler extends BaseHandler {

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final FileManager fileManager;

    public UsersHandler(UserDao userDao, SessionDao sessionDao, FileManager fileManager) {
        this.userDao = userDao;
        this.fileManager = fileManager;
        this.sessionDao = sessionDao;
    }

    @GET
    @Path("")
    public Response getUsers(@QueryParam("_filters") String filterParam,
                                 @QueryParam("_page") int page,
                                 @QueryParam("_perPage") int size,
                                 @QueryParam("_sortField") String sortField,
                                 @QueryParam("_sortDir") String sortOrder) {
        if (filterParam != null) {
            Filter filter = JsonParser.readAny(filterParam, Filter.class);
            filterParam = filter == null ? null : filter.name;
        }
        List<User> users = userDao.searchByUsername(filterParam);
        return appendTotalCountHeader(
                ok(sort(users, sortField, sortOrder), page, size), users.size()
        );
    }

    @GET
    @Path("/{name}")
    public Response getUserByName(@PathParam("name") String name) {
        return ok(userDao.getUsers().get(name));
    }

    @PUT
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public Response updateUser(@PathParam("name") String name,
                                   User updatedUser) {

        log.debug("Deleting user {}", name);
        User oldUser = userDao.getByName(name);

        //if pass was changed, cal hash.
        if (!updatedUser.pass.equals(oldUser.pass)) {
            log.debug("Updating pass for {}.", updatedUser.name);
            updatedUser.pass = SHA256Util.makeHash(updatedUser.pass, updatedUser.name);
        }

        userDao.add(updatedUser);
        log.debug("Adding new user {}", updatedUser.name);


        return ok(updatedUser);
    }

    @PUT
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Path("/{name}/changePass")
    public Response updateUser(@PathParam("name") String name,
                               UserPassPojo userPassPojo) {

        log.debug("Updating pass for user {}", name);
        User user = userDao.getByName(name);

        if (user == null) {
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        user.pass = userPassPojo.pass;

        return ok();
    }

    @DELETE
    @Path("/{name}")
    public Response deleteUserByName(@PathParam("name") String name) {
        User user = userDao.delete(name);
        if (user == null) {
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        if (!fileManager.delete(name)) {
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        Session session = sessionDao.userSession.get(new User(name, null));
        session.closeAll();

        log.info("User {} successfully removed.", name);

        return ok();
    }

}

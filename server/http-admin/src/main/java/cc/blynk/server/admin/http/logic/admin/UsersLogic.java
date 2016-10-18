package cc.blynk.server.admin.http.logic.admin;

import cc.blynk.core.http.MediaType;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.DELETE;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.PUT;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.PathParam;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.core.http.model.Filter;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.utils.HttpLogicUtil;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.SHA256Util;

import java.util.List;

import static cc.blynk.core.http.Response.appendTotalCountHeader;
import static cc.blynk.core.http.Response.ok;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.12.15.
 */
@Path("/users")
public class UsersLogic extends HttpLogicUtil {

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final FileManager fileManager;
    private final ProfileSaverWorker profileSaverWorker;

    public UsersLogic(UserDao userDao, SessionDao sessionDao, FileManager fileManager, ProfileSaverWorker profileSaverWorker) {
        this.userDao = userDao;
        this.fileManager = fileManager;
        this.sessionDao = sessionDao;
        this.profileSaverWorker = profileSaverWorker;
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

        List<User> users = userDao.searchByUsername(filterParam, AppName.ALL);
        return appendTotalCountHeader(
                ok(sort(users, sortField, sortOrder), page, size), users.size()
        );
    }

    @GET
    @Path("/{id}")
    public Response getUserByName(@PathParam("id") String id) {
        String[] parts = id.split("-");
        String name = parts[0];
        String appName = parts[1];
        return ok(userDao.getByName(name, appName));
    }

    @GET
    @Path("/names/getAll")
    public Response getAllUserNames() {
        return ok(userDao.users.keySet());
    }

    @GET
    @Path("/trigger/saveAll")
    public Response saveAll() {
        List<User> users = profileSaverWorker.saveAll();
        return ok("Saved users : " + users.size());
    }

    @PUT
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response updateUser(@PathParam("id") String id,
                                   User updatedUser) {

        log.debug("Updating user {}", id);

        String[] parts = id.split("-");
        String name = parts[0];
        String appName = parts[1];

        User oldUser = userDao.getByName(name, appName);

        //if pass was changed, cal hash.
        if (!updatedUser.pass.equals(oldUser.pass)) {
            log.debug("Updating pass for {}.", updatedUser.name);
            updatedUser.pass = SHA256Util.makeHash(updatedUser.pass, updatedUser.name);
        }

        userDao.add(updatedUser);
        updatedUser.lastModifiedTs = System.currentTimeMillis();
        log.debug("Adding new user {}", updatedUser.name);


        return ok(updatedUser);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUserByName(@PathParam("id") String id) {
        String[] parts = id.split("-");
        String name = parts[0];
        String appName = parts[1];

        User user = userDao.delete(name, appName);
        if (user == null) {
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        if (!fileManager.delete(name, appName)) {
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        Session session = sessionDao.userSession.get(new UserKey(user));
        session.closeAll();

        log.info("User {} successfully removed.", name);

        return ok();
    }

}

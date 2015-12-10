package cc.blynk.server.handlers.http.admin.handlers;

import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.http.Filter;
import cc.blynk.server.handlers.http.HttpResponse;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.utils.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import java.util.List;

import static cc.blynk.server.handlers.http.ResponseGenerator.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.12.15.
 */
@Path("/admin/users")
public class UsersHandler extends BaseHandler {

    private static final Logger log = LogManager.getLogger(UsersHandler.class);

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
    public HttpResponse getUsers(@QueryParam("_filters") String filterParam,
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
                makeResponse(sort(users, sortField, sortOrder), page, size), users.size()
        );
    }

    @GET
    @Path("/{name}")
    public HttpResponse getUserByName(@PathParam("name") String name) {
        return makeResponse(userDao.getUsers().get(name));
    }

    @DELETE
    @Path("/{name}")
    public HttpResponse deleteUserByName(@PathParam("name") String name) {
        User user = userDao.deleteUser(name);
        if (user == null) {
            return new HttpResponse(HTTP_1_1, NOT_FOUND);
        }

        if (!fileManager.delete(name)) {
            return new HttpResponse(HTTP_1_1, NOT_FOUND);
        }

        Session session = sessionDao.userSession.get(new User(name, null));
        session.closeAll();

        log.info("User {} successfully removed.", name);

        return new HttpResponse(HTTP_1_1, OK);
    }

}

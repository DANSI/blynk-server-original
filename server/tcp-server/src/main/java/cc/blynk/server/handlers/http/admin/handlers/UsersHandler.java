package cc.blynk.server.handlers.http.admin.handlers;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.http.Filter;
import cc.blynk.server.handlers.http.HttpResponse;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.utils.JsonParser;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.Collection;

import static cc.blynk.server.handlers.http.ResponseGenerator.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.12.15.
 */
@Path("/admin")
public class UsersHandler extends BaseHandler {

    private final GlobalStats stats;
    private final SessionDao sessionDao;
    private final UserDao userDao;

    public UsersHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.stats = globalStats;
    }

    @GET
    @Path("/users")
    public HttpResponse getUsers(@QueryParam("_filters") String filterParam,
                                 @QueryParam("_page") int page,
                                 @QueryParam("_perPage") int size) {
        if (filterParam != null) {
            Filter filter = JsonParser.readAny(filterParam, Filter.class);
            filterParam = filter == null ? null : filter.name;
        }
        Collection<User> users = userDao.saerchByUsername(filterParam);
        return appendTotalCountHeader(
                makeResponse(users, page, size), users.size()
        );
    }

    @GET
    @Path("/users/{name}")
    public HttpResponse getUserByName(@PathParam("name") String name) {
        return makeResponse(userDao.getUsers().get(name));
    }

}

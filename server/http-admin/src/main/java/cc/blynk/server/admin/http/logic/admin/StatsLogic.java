package cc.blynk.server.admin.http.logic.admin;

import cc.blynk.server.admin.http.response.RequestPerSecondResponse;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.core.stats.Stat;
import cc.blynk.server.handlers.http.rest.Response;
import cc.blynk.server.handlers.http.utils.LogicHelper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cc.blynk.server.handlers.http.rest.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/stats")
public class StatsLogic extends LogicHelper {

    private final GlobalStats stats;
    private final SessionDao sessionDao;
    private final UserDao userDao;

    public StatsLogic(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.stats = globalStats;
    }

    @GET
    @Path("/realtime")
    public Response getReatime() {
       return ok(Collections.singletonList(Stat.calcStats(sessionDao, userDao, stats, false)));
    }

    @GET
    @Path("/requestsPerUser")
    public Response getRequestPerUser(@QueryParam("_sortField") String sortField,
                                          @QueryParam("_sortDir") String sortOrder) {
        List<RequestPerSecondResponse> res = new ArrayList<>();
        for (Map.Entry<User, Session> entry : sessionDao.userSession.entrySet()) {
            Session session = entry.getValue();

            int appReqRate = session.getAppRequestRate();
            int hardReqRate = session.getHardRequestRate();

            if (appReqRate > 0 || hardReqRate > 0) {
                res.add(new RequestPerSecondResponse(entry.getKey().name, appReqRate, hardReqRate));
            }
        }
        return ok(sort(res, sortField, sortOrder));
    }

    @GET
    @Path("/messages")
    public Response getMessages(@QueryParam("_sortField") String sortField,
                                    @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(Stat.calcStats(sessionDao, userDao, stats, false).messages), sortField, sortOrder));
    }

    @GET
    @Path("/widgets")
    public Response getWidgets(@QueryParam("_sortField") String sortField,
                                   @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getWidgetsUsage()), sortField, sortOrder));
    }

    @GET
    @Path("/projectsPerUser")
    public Response getProjectsPerUser(@QueryParam("_sortField") String sortField,
                                           @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getProjectsPerUser()), sortField, sortOrder, true));
    }

    @GET
    @Path("/boards")
    public Response getBoards(@QueryParam("_sortField") String sortField,
                                    @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getBoardsUsage()), sortField, sortOrder));
    }

    @GET
    @Path("/filledSpace")
    public Response getFilledSpace(@QueryParam("_sortField") String sortField,
                                  @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getFilledSpace()), sortField, sortOrder, true));
    }

}

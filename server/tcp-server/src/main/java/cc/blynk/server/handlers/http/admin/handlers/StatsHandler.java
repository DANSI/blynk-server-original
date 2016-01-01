package cc.blynk.server.handlers.http.admin.handlers;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.http.admin.response.RequestPerSecondResponse;
import cc.blynk.server.handlers.http.helpers.Response;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.StatsWorker;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cc.blynk.server.handlers.http.helpers.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/stats")
public class StatsHandler extends BaseHandler {

    private final GlobalStats stats;
    private final SessionDao sessionDao;
    private final UserDao userDao;

    public StatsHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.stats = globalStats;
    }

    @GET
    @Path("/realtime")
    public Response getReatime() {
       return ok(Collections.singletonList(StatsWorker.calcStats(sessionDao, userDao, stats, false)));
    }

    @GET
    @Path("/requestsPerUser")
    public Response getRequestPerUser(@QueryParam("_sortField") String sortField,
                                          @QueryParam("_sortDir") String sortOrder) {
        List<RequestPerSecondResponse> res = new ArrayList<>();
        for (Map.Entry<User, Session> entry : sessionDao.getUserSession().entrySet()) {
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
        return ok(sort(convertMapToPair(StatsWorker.calcStats(sessionDao, userDao, stats, false).messages), sortField, sortOrder));
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

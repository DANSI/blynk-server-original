package cc.blynk.server.handlers.http.admin.handlers;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.http.HttpResponse;
import cc.blynk.server.handlers.http.admin.response.RequestPerSecondResponse;
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

import static cc.blynk.server.handlers.http.ResponseGenerator.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/admin/stats")
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
    public HttpResponse getReatime() {
       return makeResponse(Collections.singletonList(StatsWorker.calcStats(sessionDao, userDao, stats, false)));
    }

    @GET
    @Path("/requestsPerUser")
    public HttpResponse getRequestPerUser(@QueryParam("_sortField") String sortField,
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
        return makeResponse(sort(res, sortField, sortOrder));
    }

    @GET
    @Path("/messages")
    public HttpResponse getMessages(@QueryParam("_sortField") String sortField,
                                    @QueryParam("_sortDir") String sortOrder) {
        return makeResponse(sort(convertMapToPair(StatsWorker.calcStats(sessionDao, userDao, stats, false).messages), sortField, sortOrder));
    }

    @GET
    @Path("/widgets")
    public HttpResponse getWidgets(@QueryParam("_sortField") String sortField,
                                   @QueryParam("_sortDir") String sortOrder) {
        return makeResponse(sort(convertMapToPair(userDao.getWidgetsUsage()), sortField, sortOrder));
    }

    @GET
    @Path("/projectsPerUser")
    public HttpResponse getProjectsPerUser(@QueryParam("_sortField") String sortField,
                                           @QueryParam("_sortDir") String sortOrder) {
        return makeResponse(sort(convertMapToPair(userDao.getProjectsPerUser()), sortField, sortOrder, true));
    }

    @GET
    @Path("/boards")
    public HttpResponse getBoards(@QueryParam("_sortField") String sortField,
                                    @QueryParam("_sortDir") String sortOrder) {
        return makeResponse(sort(convertMapToPair(userDao.getBoardsUsage()), sortField, sortOrder));
    }

    @GET
    @Path("/filledSpace")
    public HttpResponse getFilledSpace(@QueryParam("_sortField") String sortField,
                                  @QueryParam("_sortDir") String sortOrder) {
        return makeResponse(sort(convertMapToPair(userDao.getFilledSpace()), sortField, sortOrder, true));
    }

}

package cc.blynk.server.admin.http.logic.admin;

import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.server.Holder;
import cc.blynk.server.admin.http.response.RequestPerSecondResponse;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.core.stats.Stat;
import cc.blynk.utils.HttpLogicUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cc.blynk.core.http.Response.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/stats")
public class StatsLogic extends HttpLogicUtil {

    private final GlobalStats stats;
    private final SessionDao sessionDao;
    private final UserDao userDao;
    private final FileManager fileManager;

    public StatsLogic(Holder holder) {
        this.userDao = holder.userDao;
        this.sessionDao = holder.sessionDao;
        this.stats = holder.stats;
        this.fileManager = holder.fileManager;
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
        for (Map.Entry<UserKey, Session> entry : sessionDao.userSession.entrySet()) {
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
    @Path("/facebookLogins")
    public Response getFacebookLogins(@QueryParam("_sortField") String sortField,
                              @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getFacebookLogin()), sortField, sortOrder));
    }

    @GET
    @Path("/filledSpace")
    public Response getFilledSpace(@QueryParam("_sortField") String sortField,
                                  @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getFilledSpace()), sortField, sortOrder, true));
    }

    @GET
    @Path("/userProfileSize")
    public Response getUserProfileSize(@QueryParam("_sortField") String sortField,
                                   @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(fileManager.getUserProfilesSize()), sortField, sortOrder, true));
    }


    @GET
    @Path("/webHookHosts")
    public Response getWebHookHosts(@QueryParam("_sortField") String sortField,
                                       @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getWebHookHosts()), sortField, sortOrder, true));
    }

}

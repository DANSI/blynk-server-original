package cc.blynk.server.admin.http.logic;

import cc.blynk.core.http.CookiesBaseHttpHandler;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.server.Holder;
import cc.blynk.server.admin.http.response.IpNameResponse;
import cc.blynk.server.admin.http.response.RequestPerSecondResponse;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.core.stats.model.Stat;
import io.netty.channel.ChannelHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cc.blynk.core.http.Response.ok;
import static cc.blynk.core.http.utils.AdminHttpUtil.convertMapToPair;
import static cc.blynk.core.http.utils.AdminHttpUtil.convertObjectToMap;
import static cc.blynk.core.http.utils.AdminHttpUtil.sort;
import static cc.blynk.core.http.utils.AdminHttpUtil.sortStringAsInt;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/stats")
@ChannelHandler.Sharable
public class StatsLogic extends CookiesBaseHttpHandler {

    private final UserDao userDao;
    private final FileManager fileManager;
    private final BlockingIOProcessor blockingIOProcessor;
    private final GlobalStats globalStats;
    private final ReportScheduler reportScheduler;

    public StatsLogic(Holder holder, String rootPath) {
        super(holder, rootPath);
        this.userDao = holder.userDao;
        this.fileManager = holder.fileManager;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.globalStats = holder.stats;
        this.reportScheduler = holder.reportScheduler;
    }

    @GET
    @Path("/realtime")
    public Response getReatime() {
       return ok(Collections.singletonList(
               new Stat(sessionDao, userDao, blockingIOProcessor, globalStats, reportScheduler, false)));
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
                res.add(new RequestPerSecondResponse(entry.getKey().email, appReqRate, hardReqRate));
            }
        }
        return ok(sort(res, sortField, sortOrder));
    }

    @GET
    @Path("/messages")
    public Response getMessages(@QueryParam("_sortField") String sortField,
                                    @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertObjectToMap(
                new Stat(sessionDao, userDao, blockingIOProcessor, globalStats, reportScheduler, false).commands),
                sortField, sortOrder));
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
        return ok(sortStringAsInt(convertMapToPair(userDao.getProjectsPerUser()), sortField, sortOrder));
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
        return ok(sortStringAsInt(convertMapToPair(userDao.getFilledSpace()), sortField, sortOrder));
    }

    @GET
    @Path("/userProfileSize")
    public Response getUserProfileSize(@QueryParam("_sortField") String sortField,
                                       @QueryParam("_sortDir") String sortOrder) {
        return ok(sortStringAsInt(convertMapToPair(fileManager.getUserProfilesSize()), sortField, sortOrder));
    }


    @GET
    @Path("/webHookHosts")
    public Response getWebHookHosts(@QueryParam("_sortField") String sortField,
                                    @QueryParam("_sortDir") String sortOrder) {
        return ok(sortStringAsInt(convertMapToPair(userDao.getWebHookHosts()), sortField, sortOrder));
    }

    @GET
    @Path("/ips")
    public Response getIps(@QueryParam("_filters") String filterParam,
                           @QueryParam("_page") int page,
                           @QueryParam("_perPage") int size,
                           @QueryParam("_sortField") String sortField,
                           @QueryParam("_sortDir") String sortOrder) {

        if (filterParam != null) {
            IpFilter filter = JsonParser.readAny(filterParam, IpFilter.class);
            filterParam = filter == null ? null : filter.ip;
        }

        return ok(sort(searchByIP(filterParam), sortField, sortOrder));
    }

    private static class IpFilter {
        public String ip;
    }

    private List<IpNameResponse> searchByIP(String ip) {
        Set<IpNameResponse> res = new HashSet<>();
        int counter = 0;

        for (User user : userDao.users.values()) {
            if (user.lastLoggedIP != null) {
                String name = user.email + "-" + user.appName;
                if (ip == null) {
                    res.add(new IpNameResponse(counter++, name, user.lastLoggedIP, "app"));
                    for (DashBoard dashBoard : user.profile.dashBoards) {
                        for (Device device : dashBoard.devices) {
                            if (device.lastLoggedIP != null) {
                                res.add(new IpNameResponse(counter++, name, device.lastLoggedIP, "hard"));
                            }
                        }
                    }
                } else {
                    if (user.lastLoggedIP.contains(ip) || deviceContains(user, ip)) {
                        res.add(new IpNameResponse(counter++, name, user.lastLoggedIP, "hard"));
                    }
                }
            }
        }

        return new ArrayList<>(res);
    }

    private boolean deviceContains(User user, String ip) {
        for (DashBoard dash : user.profile.dashBoards) {
            for (Device device : dash.devices) {
                if (device.lastLoggedIP != null && device.lastLoggedIP.contains(ip)) {
                    return true;
                }
            }
        }
        return false;
    }


}

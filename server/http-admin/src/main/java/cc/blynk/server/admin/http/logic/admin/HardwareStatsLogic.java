package cc.blynk.server.admin.http.logic.admin;

import cc.blynk.core.http.Response;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.utils.HttpLogicUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static cc.blynk.core.http.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/hardwareInfo")
public class HardwareStatsLogic extends HttpLogicUtil {

    private final UserDao userDao;

    public HardwareStatsLogic(UserDao userDao) {
        this.userDao = userDao;
    }

    @GET
    @Path("/version")
    public Response getLibraryVersion(@QueryParam("_sortField") String sortField,
                                           @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getLibraryVersion()), sortField, sortOrder, true));
    }

    @GET
    @Path("/cpuType")
    public Response getBoards(@QueryParam("_sortField") String sortField,
                                    @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getCpuType()), sortField, sortOrder));
    }

    @GET
    @Path("/connectionType")
    public Response getFacebookLogins(@QueryParam("_sortField") String sortField,
                              @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getConnectionType()), sortField, sortOrder));
    }

    @GET
    @Path("/boards")
    public Response getHardwareBoards(@QueryParam("_sortField") String sortField,
                                      @QueryParam("_sortDir") String sortOrder) {
        return ok(sort(convertMapToPair(userDao.getHardwareBoards()), sortField, sortOrder));
    }

}

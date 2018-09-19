package cc.blynk.server.admin.http.logic;

import cc.blynk.core.http.CookiesBaseHttpHandler;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.UserDao;
import io.netty.channel.ChannelHandler;

import static cc.blynk.core.http.Response.ok;
import static cc.blynk.core.http.utils.AdminHttpUtil.convertMapToPair;
import static cc.blynk.core.http.utils.AdminHttpUtil.sort;
import static cc.blynk.core.http.utils.AdminHttpUtil.sortStringAsInt;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/hardwareInfo")
@ChannelHandler.Sharable
public class HardwareStatsLogic extends CookiesBaseHttpHandler {

    private final UserDao userDao;

    public HardwareStatsLogic(Holder holder, String rootPath) {
        super(holder, rootPath);
        this.userDao = holder.userDao;
    }

    @GET
    @Path("/blynkVersion")
    public Response getLibraryVersion(@QueryParam("_sortField") String sortField,
                                      @QueryParam("_sortDir") String sortOrder) {
        return ok(sortStringAsInt(convertMapToPair(userDao.getLibraryVersion()), sortField, sortOrder));
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

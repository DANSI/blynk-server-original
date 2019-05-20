package cc.blynk.server.admin.http.logic;

import cc.blynk.core.http.CookiesBaseHttpHandler;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.DELETE;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.PUT;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.PathParam;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.core.http.model.Filter;
import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.db.DBManager;
import cc.blynk.utils.SHA256Util;
import cc.blynk.utils.TokenGeneratorUtil;
import cc.blynk.utils.http.MediaType;
import cc.blynk.utils.validators.BlynkEmailValidator;
import io.netty.channel.ChannelHandler;

import java.util.List;

import static cc.blynk.core.http.Response.appendTotalCountHeader;
import static cc.blynk.core.http.Response.badRequest;
import static cc.blynk.core.http.Response.notFound;
import static cc.blynk.core.http.Response.ok;
import static cc.blynk.core.http.utils.AdminHttpUtil.sort;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.12.15.
 */
@Path("/users")
@ChannelHandler.Sharable
public class UsersLogic extends CookiesBaseHttpHandler {

    private final UserDao userDao;
    private final FileManager fileManager;
    private final DBManager dbManager;
    private final ReportingDiskDao reportingDao;

    public UsersLogic(Holder holder, String rootPath) {
        super(holder, rootPath);
        this.userDao = holder.userDao;
        this.fileManager = holder.fileManager;
        this.dbManager = holder.dbManager;
        this.reportingDao = holder.reportingDiskDao;
    }

    //for tests only
    public UsersLogic(UserDao userDao, SessionDao sessionDao, DBManager dbManager,
                      FileManager fileManager, TokenManager tokenManager,
                      ReportingDiskDao reportingDao, String rootPath) {
        super(tokenManager, sessionDao, null, rootPath);
        this.userDao = userDao;
        this.fileManager = fileManager;
        this.dbManager = dbManager;
        this.reportingDao = reportingDao;
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

        List<User> users = userDao.searchByUsername(filterParam, null);
        return appendTotalCountHeader(
                ok(sort(users, sortField, sortOrder), page, size), users.size()
        );
    }

    @GET
    @Path("/{id}")
    public Response getUserByName(@PathParam("id") String id) {
        String[] parts =  slitByLast(id);
        String email = parts[0];
        String appName = parts[1];
        User user = userDao.getByName(email, appName);
        if (user == null) {
            return notFound();
        }
        return ok(user);
    }

    @GET
    @Path("/names/getAll")
    public Response getAllUserNames() {
        return ok(userDao.users.keySet());
    }

    @GET
    @Path("/token/assign")
    public Response assignToken(@QueryParam("old") String oldToken, @QueryParam("new") String newToken) {
        TokenValue tokenValue = tokenManager.getTokenValueByToken(oldToken);

        if (tokenValue == null) {
            return badRequest("This token not exists.");
        }

        tokenManager.assignToken(tokenValue.user, tokenValue.dash, tokenValue.device, newToken);
        return ok();
    }

    @GET
    @Path("/token/force")
    public Response forceToken(@QueryParam("email") String email,
                               @QueryParam("app") String app,
                               @QueryParam("dashId") int dashId,
                               @QueryParam("deviceId") int deviceId,
                               @QueryParam("new") String newToken) {

        User user = userDao.getUsers().get(new UserKey(email, app));

        if (user == null) {
            return badRequest("No user with such email.");
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        Device device = user.profile.getDeviceById(dash, deviceId);

        tokenManager.assignToken(user, dash, device, newToken);
        return ok();
    }

    @PUT
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response updateUser(@PathParam("id") String id,
                               User updatedUser) {

        log.debug("Updating user {}", id);

        String[] parts =  slitByLast(id);
        String name = parts[0];
        String appName = parts[1];

        User oldUser = userDao.getByName(name, appName);

        //name was changed, but not password - do not allow this.
        //as name is used as salt for pass generation
        if (!updatedUser.email.equals(oldUser.email) && updatedUser.pass.equals(oldUser.pass)) {
            return badRequest("You need also change password when changing email.");
        }

        if (BlynkEmailValidator.isNotValidEmail(updatedUser.email)) {
            return badRequest("Wring email address.");
        }

        //user name was changed
        if (!updatedUser.email.equals(oldUser.email)) {
            deleteUserByName(id);
            for (DashBoard dashBoard : oldUser.profile.dashBoards) {
                for (Device device : dashBoard.devices) {
                    String token;
                    if (device.token == null) {
                        token = TokenGeneratorUtil.generateNewToken();
                    } else {
                        token = device.token;
                    }
                    tokenManager.assignToken(updatedUser, dashBoard, device, token);
                }
            }
        }

        //if pass was changed, call hash function.
        if (!updatedUser.pass.equals(oldUser.pass)) {
            log.debug("Updating pass for {}.", updatedUser.email);
            updatedUser.pass = SHA256Util.makeHash(updatedUser.pass, updatedUser.email);
        }

        userDao.add(updatedUser);

        for (DashBoard dash : updatedUser.profile.dashBoards) {
            for (Device device : dash.devices) {
                if (device.token != null) {
                    tokenManager.updateRegularCache(device.token, updatedUser, dash, device);
                }
            }
            if (dash.sharedToken != null) {
                tokenManager.updateSharedCache(dash.sharedToken, updatedUser, dash.id);
            }
        }

        updatedUser.lastModifiedTs = System.currentTimeMillis();
        log.debug("Adding new user {}", updatedUser.email);

        return ok(updatedUser);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUserByName(@PathParam("id") String id) {
        String[] parts =  slitByLast(id);
        String email = parts[0];
        String appName = parts[1];

        UserKey userKey = new UserKey(email, appName);
        User user = userDao.delete(userKey);
        if (user == null) {
            return notFound();
        }

        if (!fileManager.delete(email, appName)) {
            return notFound();
        }

        reportingDao.delete(user);

        dbManager.deleteUser(userKey);

        Session session = sessionDao.userSession.remove(userKey);
        if (session != null) {
            session.closeAll();
        }

        log.info("User {} successfully removed.", email);

        return ok();
    }

    private String[] slitByLast(String id) {
        int i = id.lastIndexOf("-");
        return new String[] {
                id.substring(0, i),
                id.substring(i + 1)
        };
    }

}

package cc.blynk.server.api.http.logic.ide;

import cc.blynk.core.http.BaseHttpHandler;
import cc.blynk.core.http.MediaType;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.POST;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.redis.RedisClient;
import io.netty.channel.ChannelHandler;

import java.util.Map;

import static cc.blynk.core.http.Response.*;

/**
 * @author gig.
 */
@Path("/ide")
@ChannelHandler.Sharable
public class IDEAuthLogic extends BaseHttpHandler {

    private final UserDao userDao;
    private final RedisClient redisClient;
    protected static final String IDE_AUTHORIZE_ENDPOINT = "/ide/authorize";

    //for tests only
    protected IDEAuthLogic(UserDao userDao, RedisClient redisClient) {
        super(null, null, null, "");
        this.userDao = userDao;
        this.redisClient = redisClient;
    }

    public IDEAuthLogic(Holder holder) {
        super(holder, "");
        this.userDao = holder.userDao;
        this.redisClient = holder.redisClient;
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Path("/authorize")
    public Response authorize(Map<String, String> data) {

        final String email = data.get("email");
        final String password = data.get("password");

        if (email == null || password == null) {
            return badRequest();
        }

        User user = userDao.getByName(email, AppName.BLYNK);

        if (user == null) {
            String userServer = redisClient.getServerByUser(email);
            if (userServer != null) {
                return redirect("//" + userServer + IDE_AUTHORIZE_ENDPOINT);
            }
        }

        if (user == null || !password.equals(user.pass)) {
            return badRequest();
        }

        return ok(user);
    }

}

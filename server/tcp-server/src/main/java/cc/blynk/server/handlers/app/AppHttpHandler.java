package cc.blynk.server.handlers.app;

import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.http.helpers.Response;
import cc.blynk.server.handlers.http.helpers.SimplePin;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.Widget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static cc.blynk.server.handlers.http.helpers.ResponseGenerator.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.12.15.
 */
@Path("/app")
public class AppHttpHandler {

    static final Logger log = LogManager.getLogger(AppHttpHandler.class);

    private final UserDao userDao;
    private final SessionDao sessionDao;

    public AppHttpHandler(UserDao userDao, SessionDao sessionDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
    }

    @GET
    @Path("/{token}/widget/{pin}")
    public Response getWidgetPinData(@PathParam("token") String token,
                                     @PathParam("pin") String pin) {

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        DashBoard dashBoard = user.profile.getDashById(dashId);

        SimplePin simplePin;

        try {
            simplePin = new SimplePin(pin);
        } catch (NumberFormatException e) {
            log.error("Wrong pin format. {}", pin);
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        Widget widget = dashBoard.findWidgetByPin(simplePin.pin, simplePin.pinType);

        if (widget == null) {
            log.error("Requested pin {} not found. User {}", pin, user.name);
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        return makeResponse(widget.getJsonValue());
    }

    @PUT
    @Path("/{token}/widget/{pin}")
    public Response updateWidgetPinData(@PathParam("token") String token,
                                        @PathParam("pin") String pin) {

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        DashBoard dashBoard = user.profile.getDashById(dashId);

        SimplePin simplePin = new SimplePin(pin);

        Widget widget = dashBoard.findWidgetByPin(simplePin.pin, simplePin.pinType);

        if (widget == null) {
            log.error("Requested pin {} not found. User {}", pin, user.name);
            return new Response(HTTP_1_1, NOT_FOUND);
        }

        return makeResponse(widget.getJsonValue());
    }

}

package cc.blynk.server.handlers.app;

import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.http.helpers.Response;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.model.widgets.Widget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static cc.blynk.server.handlers.http.helpers.ResponseGenerator.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.12.15.
 */
@Path("/app")
public class AppHttpHandler {

    static final Logger log = LogManager.getLogger(AppHttpHandler.class);

    private final UserDao userDao;

    public AppHttpHandler(UserDao userDao) {
        this.userDao = userDao;
    }

    @GET
    @Path("/{token}/widget/{pin}")
    public Response getWidgetPinData(@PathParam("token") String token,
                                     @PathParam("pin") String pinString) {

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.notFound();
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.notFound();
        }

        DashBoard dashBoard = user.profile.getDashById(dashId);

        PinType pinType;
        byte pin;

        try {
            pinType = PinType.getPingType(pinString.charAt(0));
            pin = Byte.parseByte(pinString.substring(1));
        } catch (NumberFormatException e) {
            log.error("Wrong pin format. {}", pinString);
            return Response.notFound();
        }

        Widget widget = dashBoard.findWidgetByPin(pin, pinType);

        if (widget == null) {
            log.error("Requested pin {} not found. User {}", pinString, user.name);
            return Response.notFound();
        }

        return makeResponse(widget.getJsonValue());
    }

    @PUT
    @Path("/{token}/widget/{pin}")
    @Consumes(value = MediaType.APPLICATION_JSON)
    public Response updateWidgetPinData(@PathParam("token") String token,
                                        @PathParam("pin") String pinString,
                                        String[] pinValues) {

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.notFound();
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.notFound();
        }

        DashBoard dashBoard = user.profile.getDashById(dashId);

        PinType pinType;
        byte pin;

        try {
            pinType = PinType.getPingType(pinString.charAt(0));
            pin = Byte.parseByte(pinString.substring(1));
        } catch (NumberFormatException e) {
            log.error("Wrong pin format. {}", pinString);
            return Response.notFound();
        }

        Widget widget = dashBoard.findWidgetByPin(pin, pinType);

        if (widget == null) {
            log.error("Requested pin {} not found. User {}", pinString, user.name);
            return Response.notFound();
        }

        widget.updateIfSame(new HardwareBody(pinType, pin, pinValues));

        return Response.noContent();
    }

}

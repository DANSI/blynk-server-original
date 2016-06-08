package cc.blynk.server.api.http.logic;

import cc.blynk.core.http.Response;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.pojo.EmailPojo;
import cc.blynk.server.api.http.pojo.PushMessagePojo;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMMessage;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.ios.IOSGCMMessage;
import cc.blynk.utils.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static cc.blynk.core.http.Response.*;
import static cc.blynk.server.core.protocol.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.12.15.
 */
@Path("/")
public class HttpAPILogic {

    private static final Logger log = LogManager.getLogger(HttpAPILogic.class);

    private final UserDao userDao;
    private final BlockingIOProcessor blockingIOProcessor;
    private final SessionDao sessionDao;
    private final GlobalStats globalStats;
    private final MailWrapper mailWrapper;
    private final GCMWrapper gcmWrapper;
    private final ReportingDao reportingDao;

    public HttpAPILogic(Holder holder) {
        this(holder.userDao, holder.sessionDao, holder.blockingIOProcessor, holder.mailWrapper, holder.gcmWrapper, holder.reportingDao, holder.stats);
    }

    private HttpAPILogic(UserDao userDao, SessionDao sessionDao, BlockingIOProcessor blockingIOProcessor,
                         MailWrapper mailWrapper, GCMWrapper gcmWrapper, ReportingDao reportingDao, GlobalStats globalStats) {
        this.userDao = userDao;
        this.blockingIOProcessor = blockingIOProcessor;
        this.sessionDao = sessionDao;
        this.globalStats = globalStats;
        this.mailWrapper = mailWrapper;
        this.gcmWrapper = gcmWrapper;
        this.reportingDao = reportingDao;
    }

    @GET
    @Path("{token}/project")
    public Response getDashboard(@PathParam("token") String token) {
        globalStats.mark(HTTP_GET_PROJECT);

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.badRequest("Didn't find dash id for token.");
        }

        DashBoard dashBoard = user.profile.getDashById(dashId);

        return ok(dashBoard.toString());
    }

    @GET
    @Path("{token}/isHardwareConnected")
    public Response isHardwareConnected(@PathParam("token") String token) {
        globalStats.mark(HTTP_IS_HARDWARE_CONNECTED);

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.badRequest("Didn't find dash id for token.");
        }

        final Session session = sessionDao.userSession.get(user);

        return ok(session.isHardwareConnected(dashId));
    }

    @GET
    @Path("{token}/isAppConnected")
    public Response isAppConnected(@PathParam("token") String token) {
        globalStats.mark(HTTP_IS_APP_CONNECTED);

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.badRequest("Didn't find dash id for token.");
        }

        final DashBoard dashBoard = user.profile.getDashById(dashId);

        final Session session = sessionDao.userSession.get(user);

        return ok(dashBoard.isActive && session.isAppConnected());
    }

    @GET
    @Path("{token}/pin/{pin}")
    public Response getWidgetPinData(@PathParam("token") String token,
                                     @PathParam("pin") String pinString) {

        globalStats.mark(HTTP_GET_PIN_DATA);

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.badRequest("Didn't find dash id for token.");
        }

        DashBoard dashBoard = user.profile.getDashById(dashId);

        PinType pinType;
        byte pin;

        try {
            pinType = PinType.getPinType(pinString.charAt(0));
            pin = Byte.parseByte(pinString.substring(1));
        } catch (NumberFormatException | IllegalCommandBodyException e) {
            log.error("Wrong pin format. {}", pinString);
            return Response.badRequest("Wrong pin format.");
        }

        Widget widget = dashBoard.findWidgetByPin(pin, pinType);

        if (widget == null) {
            log.error("Requested pin {} not found. User {}", pinString, user.name);
            return Response.badRequest("Requested pin not exists in app.");
        }

        return ok(widget.getJsonValue());
    }

    @PUT
    @Path("{token}/pin/{pin}")
    @Consumes(value = MediaType.APPLICATION_JSON)
    public Response updateWidgetPinData(@PathParam("token") String token,
                                        @PathParam("pin") String pinString,
                                        String[] pinValues) {

        globalStats.mark(HTTP_UPDATE_PIN_DATA);

        if (pinValues.length == 0) {
            log.error("No pin for update provided.");
            return Response.badRequest("No pin for update provided.");
        }

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.badRequest("Didn't find dash id for token.");
        }

        DashBoard dashBoard = user.profile.getDashById(dashId);

        PinType pinType;
        byte pin;

        try {
            pinType = PinType.getPinType(pinString.charAt(0));
            pin = Byte.parseByte(pinString.substring(1));
        } catch (NumberFormatException | IllegalCommandBodyException e) {
            log.error("Wrong pin format. {}", pinString);
            return Response.badRequest("Wrong pin format.");
        }

        Widget widget = dashBoard.findWidgetByPin(pin, pinType);
        String body;

        String pinValue = String.join(StringUtils.BODY_SEPARATOR_STRING, pinValues);

        reportingDao.process(user.name, dashId, pin, pinType, pinValue);

        if (widget == null) {
            body = Pin.makeHardwareBody(pinType, pin, pinValues);
        } else {
            widget.updateIfSame(pin, pinType, pinValue);
            if (widget instanceof OnePinWidget) {
                body = ((OnePinWidget) widget).makeHardwareBody();
            } else if (widget instanceof MultiPinWidget) {
                body = ((MultiPinWidget) widget).makeHardwareBody();
            } else {
                body = null;
            }
        }

        if (body != null) {
            Session session = sessionDao.userSession.get(user);
            if (session == null) {
                log.error("No session for user {}.", user.name);
                return Response.ok();
            }
            session.sendMessageToHardware(dashId, HARDWARE, 111, body);

            if (dashBoard.isActive) {
                //todo check for shared apps? to minimize load...
                session.sendToApps(HARDWARE, 111, dashId + StringUtils.BODY_SEPARATOR_STRING + body);
            }
        }

        return Response.ok();
    }

    @POST
    @Path("{token}/notify")
    @Consumes(value = MediaType.APPLICATION_JSON)
    public Response notify(@PathParam("token") String token,
                                        PushMessagePojo message) {

        globalStats.mark(HTTP_NOTIFY);

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.badRequest("Didn't find dash id for token.");
        }

        if (message == null || Notification.isWrongBody(message.body)) {
            log.error("Notification body is wrong. '{}'", message == null ? "" : message.body);
            return Response.badRequest("Body is empty or larger than 255 chars.");
        }

        DashBoard dash = user.profile.getDashById(dashId);

        if (!dash.isActive) {
            log.error("Project is not active.");
            return Response.badRequest("Project is not active.");
        }

        Notification notification = dash.getWidgetByType(Notification.class);

        if (notification == null || notification.hasNoToken()) {
            log.error("No notification tokens.");
            return Response.badRequest("No notification widget or widget not initialized.");
        }

        log.trace("Sending push for user {}, with message : '{}'.", user.name, message.body);
        push(user, notification, message.body, 1);

        return Response.ok();
    }

    private void push(User user, Notification widget, String body, int dashId) {
        if (widget.androidTokens.size() != 0) {
            for (String token : widget.androidTokens.values()) {
                push(user, new AndroidGCMMessage(token, widget.priority, body, dashId));
            }
        }

        if (widget.iOSTokens.size() != 0) {
            for (String token : widget.iOSTokens.values()) {
                push(user, new IOSGCMMessage(token, widget.priority, body, dashId));
            }
        }
    }

    private void push(User user, GCMMessage message) {
        blockingIOProcessor.execute(() -> {
            try {
                gcmWrapper.send(message);
            } catch (Exception e) {
                log.error("Error sending push notification on offline hardware. For user {}. Reason {}", user.name, e.getMessage());
            }
        });
    }

    @POST
    @Path("{token}/email")
    @Consumes(value = MediaType.APPLICATION_JSON)
    public Response email(@PathParam("token") String token,
                                        EmailPojo message) {

        globalStats.mark(HTTP_EMAIL);

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        Integer dashId = user.getDashIdByToken(token);

        if (dashId == null) {
            log.error("Dash id for token {} not found. User {}", token, user.name);
            return Response.badRequest("Didn't find dash id for token.");
        }

        DashBoard dash = user.profile.getDashById(dashId);

        if (!dash.isActive) {
            log.error("Project is not active.");
            return Response.badRequest("Project is not active.");
        }

        Mail mail = dash.getWidgetByType(Mail.class);

        if (mail == null) {
            log.error("No email widget.");
            return Response.badRequest("No email widget.");
        }

        if (message == null ||
                message.subj == null || message.subj.equals("") ||
                message.to == null || message.to.equals("")) {
            log.error("Email body empty. '{}'", message);
            return Response.badRequest("Email body is wrong. Missing or empty fields 'to', 'subj'.");
        }

        log.trace("Sending Mail for user {}, with message : '{}'.", user.name, message.subj);
        mail(user.name, message.to, message.subj, message.title);

        return Response.ok();
    }

    private void mail(String username, String to, String subj, String body) {
        blockingIOProcessor.execute(() -> {
            try {
                mailWrapper.send(to, subj, body);
            } catch (Exception e) {
                log.error("Error sending email from HTTP. From : '{}', to : '{}'. Reason : {}", username, to, e.getMessage());
            }
        });
    }

}

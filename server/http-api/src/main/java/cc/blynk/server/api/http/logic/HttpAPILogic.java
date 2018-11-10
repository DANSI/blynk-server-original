package cc.blynk.server.api.http.logic;

import cc.blynk.core.http.Response;
import cc.blynk.core.http.TokenBaseHttpHandler;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.EnumQueryParam;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.Metric;
import cc.blynk.core.http.annotation.POST;
import cc.blynk.core.http.annotation.PUT;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.PathParam;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.pojo.EmailPojo;
import cc.blynk.server.api.http.pojo.PinData;
import cc.blynk.server.api.http.pojo.PushMessagePojo;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.server.core.model.storage.value.SinglePinStorageValue;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.others.rtc.RTC;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.processors.EventorProcessor;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.exceptions.NoDataException;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.utils.NumberUtil;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.TokenGeneratorUtil;
import cc.blynk.utils.http.MediaType;
import io.netty.channel.ChannelHandler;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractMap;

import static cc.blynk.core.http.Response.badRequest;
import static cc.blynk.core.http.Response.ok;
import static cc.blynk.core.http.Response.redirect;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_EMAIL;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_GET_HISTORY_DATA;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_GET_PIN_DATA;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_GET_PROJECT;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_IS_APP_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_IS_HARDWARE_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_NOTIFY;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_QR;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_UPDATE_PIN_DATA;
import static cc.blynk.server.core.protocol.enums.Command.SET_WIDGET_PROPERTY;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.12.15.
 */
@Path("/")
@ChannelHandler.Sharable
public class HttpAPILogic extends TokenBaseHttpHandler {

    private static final Logger log = LogManager.getLogger(HttpAPILogic.class);
    private final BlockingIOProcessor blockingIOProcessor;
    private final MailWrapper mailWrapper;
    private final GCMWrapper gcmWrapper;
    private final ReportingDiskDao reportingDao;
    private final EventorProcessor eventorProcessor;
    private final DBManager dbManager;
    private final FileManager fileManager;
    private final String host;
    private final String httpsPort;

    public HttpAPILogic(Holder holder) {
        super(holder.tokenManager, holder.sessionDao, holder.stats, "");
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.mailWrapper = holder.mailWrapper;
        this.gcmWrapper = holder.gcmWrapper;
        this.reportingDao = holder.reportingDiskDao;
        this.eventorProcessor = holder.eventorProcessor;
        this.dbManager = holder.dbManager;
        this.fileManager = holder.fileManager;
        this.host = holder.props.host;
        this.httpsPort = holder.props.getHttpsPortAsString();
    }

    private static String makeBody(DashBoard dash, int deviceId, short pin, PinType pinType, String pinValue) {
        Widget widget = dash.findWidgetByPin(deviceId, pin, pinType);
        if (widget instanceof OnePinWidget) {
            return ((OnePinWidget) widget).makeHardwareBody();
        } else if (widget instanceof MultiPinWidget) {
            return ((MultiPinWidget) widget).makeHardwareBody(pin, pinType);
        }

        return DataStream.makeHardwareBody(pinType, pin, pinValue);
    }

    @GET
    @Path("{token}/project")
    @Metric(HTTP_GET_PROJECT)
    public Response getDashboard(@PathParam("token") String token) {
        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        return ok(JsonParser.toJsonRestrictiveDashboardForHTTP(tokenValue.dash));
    }

    @GET
    @Path("{token}/isHardwareConnected")
    @Metric(HTTP_IS_HARDWARE_CONNECTED)
    public Response isHardwareConnected(@PathParam("token") String token) {
        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;
        int dashId = tokenValue.dash.id;
        int deviceId = tokenValue.device.id;

        Session session = sessionDao.get(new UserKey(user));

        return ok(session.isHardwareConnected(dashId, deviceId));
    }

    @GET
    @Path("{token}/isAppConnected")
    @Metric(HTTP_IS_APP_CONNECTED)
    public Response isAppConnected(@PathParam("token") String token) {
        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;
        Session session = sessionDao.get(new UserKey(user));

        return ok(tokenValue.dash.isActive && session.isAppConnected());
    }

    @GET
    @Path("{token}/get/{pin}")
    @Metric(HTTP_GET_PIN_DATA)
    public Response getWidgetPinDataNew(@PathParam("token") String token,
                                        @PathParam("pin") String pinString) {
        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;
        int deviceId = tokenValue.device.id;
        DashBoard dash = tokenValue.dash;

        PinType pinType;
        short pin;

        try {
            pinType = PinType.getPinType(pinString.charAt(0));
            pin = NumberUtil.parsePin(pinString.substring(1));
        } catch (NumberFormatException | IllegalCommandBodyException e) {
            log.debug("Wrong pin format. {}", pinString);
            return badRequest("Wrong pin format.");
        }

        Widget widget = dash.findWidgetByPin(deviceId, pin, pinType);

        if (widget == null) {
            PinStorageValue value = user.profile.pinsStorage.get(
                    new DashPinStorageKey(dash.id, deviceId, pinType, pin));
            if (value == null) {
                log.debug("Requested pin {} not found. User {}", pinString, user.email);
                return badRequest("Requested pin doesn't exist in the app.");
            }
            if (value instanceof SinglePinStorageValue) {
                return ok(JsonParser.valueToJsonAsString((SinglePinStorageValue) value));
            } else {
                return ok(JsonParser.valueToJsonAsString(value.values()));
            }
        }

        if (widget instanceof DeviceTiles) {
            String value = ((DeviceTiles) widget).getValue(deviceId, pin, pinType);
            if (value == null) {
                log.debug("Requested pin {} not found. User {}", pinString, user.email);
                return badRequest("Requested pin doesn't exist in the app.");
            }
            return ok(value);
        }

        return ok(widget.getJsonValue());
    }

    @GET
    @Path("{token}/rtc")
    @Metric(HTTP_GET_PIN_DATA)
    public Response getWidgetPinData(@PathParam("token") String token) {
        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;

        RTC rtc = tokenValue.dash.getWidgetByType(RTC.class);

        if (rtc == null) {
            log.debug("Requested rtc widget not found. User {}", user.email);
            return badRequest("Requested rtc not exists in app.");
        }

        return ok(rtc.getJsonValue());
    }

    @GET
    @Path("{token}/qr")
    @Metric(HTTP_QR)
    public Response getQR(@PathParam("token") String token) {
        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        DashBoard dash = tokenValue.dash;

        String qrToken = TokenGeneratorUtil.generateNewToken();
        String json = JsonParser.toJsonRestrictiveDashboard(dash);

        blockingIOProcessor.executeDB(() -> {
            try {
                boolean insertStatus = dbManager.insertClonedProject(qrToken, json);
                if (!insertStatus && !fileManager.writeCloneProjectToDisk(qrToken, json)) {
                    log.error("Creating clone project failed for {}", tokenValue.user.email);
                }
            } catch (Exception e) {
                log.error("Error cloning project for {}.", tokenValue.user.email, e);
            }
        });

        //todo generate QR on client side.
        String cloneQrString = "blynk://token/clone/" + qrToken + "?server=" + host + "&port=" + httpsPort;
        byte[] qrDataBinary = QRCode.from(cloneQrString).to(ImageType.PNG).stream().toByteArray();
        return ok(qrDataBinary, "image/png");
    }

    @GET
    @Path("{token}/data/{pin}")
    @Metric(HTTP_GET_HISTORY_DATA)
    public Response getPinHistoryData(@PathParam("token") String token,
                                      @PathParam("pin") String pinString) {
        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;
        int dashId = tokenValue.dash.id;
        int deviceId = tokenValue.device.id;

        PinType pinType;
        short pin;

        try {
            pinType = PinType.getPinType(pinString.charAt(0));
            pin = NumberUtil.parsePin(pinString.substring(1));
        } catch (NumberFormatException | IllegalCommandBodyException e) {
            log.debug("Wrong pin format. {}", pinString);
            return badRequest("Wrong pin format.");
        }

        //todo may be optimized
        try {
            java.nio.file.Path path = reportingDao.csvGenerator.createCSV(
                    user, dashId, deviceId, pinType, pin, deviceId);
            return redirect("/" + path.getFileName().toString());
        } catch (NoDataException | IllegalStateException noData) {
            log.debug(noData.getMessage());
            return badRequest(noData.getMessage());
        } catch (Exception e) {
            log.debug("Error getting pin data.", e);
            return badRequest("Error getting pin data.");
        }
    }

    public Response updateWidgetProperty(String token,
                                         String pinString,
                                         WidgetProperty property,
                                         String value) {
        if (value == null) {
            log.debug("No properties for update provided.");
            return badRequest("No properties for update provided.");
        }

        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;
        int deviceId = tokenValue.device.id;
        DashBoard dash = tokenValue.dash;

        //todo add test for this use case
        if (!dash.isActive) {
            return badRequest("Project is not active.");
        }

        PinType pinType;
        short pin;
        try {
            pinType = PinType.getPinType(pinString.charAt(0));
            pin = NumberUtil.parsePin(pinString.substring(1));
        } catch (NumberFormatException | IllegalCommandBodyException e) {
            log.debug("Wrong pin format. {}", pinString);
            return badRequest("Wrong pin format.");
        }

        //for now supporting only virtual pins
        Widget widget = null;
        for (Widget dashWidget : dash.widgets) {
            if (dashWidget.isSame(deviceId, pin, pinType)) {
                //todo for now supporting only single property
                if (!dashWidget.setProperty(property, value)) {
                    log.debug("Property {} with value {} not supported.", property, value);
                    return badRequest("Error setting widget property.");
                }
                widget = dashWidget;
            }
        }

        if (widget == null) {
            log.debug("No widget for SetWidgetProperty command.");
            return badRequest("No widget for SetWidgetProperty command.");
        }

        Session session = sessionDao.get(new UserKey(user));
        session.sendToApps(SET_WIDGET_PROPERTY, 111, dash.id,
                deviceId, "" + pin + BODY_SEPARATOR + property + BODY_SEPARATOR + value);
        return ok();
    }

    //todo it is a bit ugly right now. could be simplified by passing map of query params.
    @GET
    @Path("{token}/update/{pin}")
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Metric(HTTP_UPDATE_PIN_DATA)
    public Response updateWidgetPinDataViaGet(@PathParam("token") String token,
                                              @PathParam("pin") String pinString,
                                              @QueryParam("value") String[] pinValues,
                                              @EnumQueryParam(WidgetProperty.class)
                                                          AbstractMap.SimpleImmutableEntry<WidgetProperty, String>
                                                          widgetProperty) {

        if (pinValues != null) {
            return updateWidgetPinData(token, pinString, pinValues);
        }
        if (widgetProperty != null) {
            return updateWidgetProperty(token, pinString, widgetProperty.getKey(), widgetProperty.getValue());
        }

        return badRequest("Wrong request format.");
    }

    @PUT
    @Path("{token}/update/{pin}")
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Metric(HTTP_UPDATE_PIN_DATA)
    public Response updateWidgetPinDataNew(@PathParam("token") String token,
                                           @PathParam("pin") String pinString,
                                           String[] pinValues) {
        return updateWidgetPinData(token, pinString, pinValues);
    }

    //todo remove later?
    @PUT
    @Path("{token}/pin/{pin}")
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Metric(HTTP_UPDATE_PIN_DATA)
    public Response updateWidgetPinData(@PathParam("token") String token,
                                        @PathParam("pin") String pinString,
                                        String[] pinValues) {

        if (pinValues.length == 0) {
            log.debug("No pin for update provided.");
            return badRequest("No pin for update provided.");
        }

        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;
        int dashId = tokenValue.dash.id;
        int deviceId = tokenValue.device.id;

        DashBoard dash = tokenValue.dash;

        PinType pinType;
        short pin;

        try {
            pinType = PinType.getPinType(pinString.charAt(0));
            pin = NumberUtil.parsePin(pinString.substring(1));
        } catch (NumberFormatException | IllegalCommandBodyException e) {
            log.debug("Wrong pin format. {}", pinString);
            return badRequest("Wrong pin format.");
        }

        final long now = System.currentTimeMillis();

        String pinValue = String.join(StringUtils.BODY_SEPARATOR_STRING, pinValues);

        reportingDao.process(user, dash, deviceId, pin, pinType, pinValue, now);

        user.profile.update(dash, deviceId, pin, pinType, pinValue, now);
        tokenValue.device.dataReceivedAt = now;

        String body = makeBody(dash, deviceId, pin, pinType, pinValue);

        Session session = sessionDao.get(new UserKey(user));
        if (session == null) {
            log.debug("No session for user {}.", user.email);
            return ok();
        }

        eventorProcessor.process(user, session, dash, deviceId, pin, pinType, pinValue, now);

        session.sendMessageToHardware(dashId, HARDWARE, 111, body, deviceId);

        if (dash.isActive) {
            session.sendToApps(HARDWARE, 111, dashId, deviceId, body);
        }

        return ok();
    }

    @PUT
    @Path("{token}/extra/pin/{pin}")
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Metric(HTTP_UPDATE_PIN_DATA)
    public Response updateWidgetPinData(@PathParam("token") String token,
                                        @PathParam("pin") String pinString,
                                        PinData[] pinsData) {

        if (pinsData.length == 0) {
            log.debug("No pin for update provided.");
            return badRequest("No pin for update provided.");
        }

        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;
        int dashId = tokenValue.dash.id;
        int deviceId = tokenValue.device.id;

        DashBoard dash = tokenValue.dash;

        PinType pinType;
        short pin;

        try {
            pinType = PinType.getPinType(pinString.charAt(0));
            pin = NumberUtil.parsePin(pinString.substring(1));
        } catch (NumberFormatException | IllegalCommandBodyException e) {
            log.debug("Wrong pin format. {}", pinString);
            return badRequest("Wrong pin format.");
        }

        for (PinData pinData : pinsData) {
            reportingDao.process(user, dash, deviceId, pin, pinType, pinData.value, pinData.timestamp);
        }

        long now = System.currentTimeMillis();
        user.profile.update(dash, deviceId, pin, pinType, pinsData[0].value, now);

        String body = makeBody(dash, deviceId, pin, pinType, pinsData[0].value);

        if (body != null) {
            Session session = sessionDao.get(new UserKey(user));
            if (session == null) {
                log.error("No session for user {}.", user.email);
                return ok();
            }
            session.sendMessageToHardware(dashId, HARDWARE, 111, body, deviceId);

            if (dash.isActive) {
                session.sendToApps(HARDWARE, 111, dashId, deviceId, body);
            }
        }

        return ok();
    }

    @POST
    @Path("{token}/notify")
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Metric(HTTP_NOTIFY)
    public Response notify(@PathParam("token") String token,
                           PushMessagePojo message) {

        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;

        if (message == null || Notification.isWrongBody(message.body)) {
            log.debug("Notification body is wrong. '{}'", message == null ? "" : message.body);
            return badRequest("Body is empty or larger than 255 chars.");
        }

        DashBoard dash = tokenValue.dash;

        if (!dash.isActive) {
            log.debug("Project is not active.");
            return badRequest("Project is not active.");
        }

        Notification notification = dash.getNotificationWidget();

        if (notification == null || notification.hasNoToken()) {
            log.debug("No notification tokens.");
            if (notification == null) {
                return badRequest("No notification widget.");
            } else {
                return badRequest("Notification widget not initialized.");
            }
        }

        log.trace("Sending push for user {}, with message : '{}'.", user.email, message.body);
        notification.push(gcmWrapper, message.body, dash.id);

        return ok();
    }

    @POST
    @Path("{token}/email")
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Metric(HTTP_EMAIL)
    public Response email(@PathParam("token") String token,
                          EmailPojo message) {

        TokenValue tokenValue = tokenManager.getTokenValueByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        DashBoard dash = tokenValue.dash;

        if (dash == null || !dash.isActive) {
            log.debug("Project is not active.");
            return badRequest("Project is not active.");
        }

        Mail mail = dash.getMailWidget();

        if (mail == null) {
            log.debug("No email widget.");
            return badRequest("No email widget.");
        }

        if (message == null
                || message.subj == null || message.subj.isEmpty()
                || message.to == null || message.to.isEmpty()) {
            log.debug("Email body empty. '{}'", message);
            return badRequest("Email body is wrong. Missing or empty fields 'to', 'subj'.");
        }

        log.trace("Sending Mail for user {}, with message : '{}'.", tokenValue.user.email, message.subj);
        mail(tokenValue.user.email, message.to, message.subj, message.title);

        return ok();
    }

    private void mail(String email, String to, String subj, String body) {
        blockingIOProcessor.execute(() -> {
            try {
                mailWrapper.sendText(to, subj, body);
            } catch (Exception e) {
                log.error("Error sending email from HTTP. From : '{}', to : '{}'. Reason : {}",
                        email, to, e.getMessage());
            }
        });
    }

}

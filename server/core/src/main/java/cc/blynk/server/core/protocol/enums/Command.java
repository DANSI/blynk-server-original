package cc.blynk.server.core.protocol.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public final class Command {

    public static final short RESPONSE = 0;

    //app commands
    public static final short REGISTER = 1;
    public static final short LOGIN = 2;
    public static final short REDEEM = 3;
    public static final short HARDWARE_CONNECTED = 4;

    public static final short GET_TOKEN = 5;
    public static final short PING = 6;
    public static final short ACTIVATE_DASHBOARD = 7;
    public static final short DEACTIVATE_DASHBOARD = 8;
    public static final short REFRESH_TOKEN = 9;
    public static final short GET_GRAPH_DATA = 10;
    public static final short GET_GRAPH_DATA_RESPONSE = 11;
    //HARDWARE commands
    public static final short TWEET = 12;
    public static final short EMAIL = 13;
    public static final short PUSH_NOTIFICATION = 14;
    public static final short BRIDGE = 15;
    public static final short HARDWARE_SYNC = 16;
    public static final short BLYNK_INTERNAL = 17;
    public static final short SMS = 18;
    public static final short SET_WIDGET_PROPERTY = 19;
    public static final short HARDWARE = 20;
    //app commands
    public static final short CREATE_DASH = 21;
    public static final short UPDATE_DASH = 22;
    public static final short DELETE_DASH = 23;
    public static final short LOAD_PROFILE_GZIPPED = 24;
    public static final short APP_SYNC = 25;
    public static final short SHARING = 26;
    public static final short ADD_PUSH_TOKEN = 27;
    public static final short EXPORT_GRAPH_DATA = 28;
    //app sharing commands
    public static final short GET_SHARE_TOKEN = 30;
    public static final short REFRESH_SHARE_TOKEN = 31;
    public static final short SHARE_LOGIN = 32;
    //app commands
    public static final short CREATE_WIDGET = 33;
    public static final short UPDATE_WIDGET = 34;
    public static final short DELETE_WIDGET = 35;

    //energy commands
    public static final short GET_ENERGY = 36;
    public static final short ADD_ENERGY = 37;

    public static final short UPDATE_PROJECT_SETTINGS = 38;

    public static final short ASSIGN_TOKEN = 39;

    public static final short GET_SERVER = 40;
    public static final short CONNECT_REDIRECT = 41;

    public static final short CREATE_DEVICE = 42;
    public static final short UPDATE_DEVICE = 43;
    public static final short DELETE_DEVICE = 44;
    public static final short GET_DEVICES = 45;

    public static final short CREATE_TAG = 46;
    public static final short UPDATE_TAG = 47;
    public static final short DELETE_TAG = 48;
    public static final short GET_TAGS = 49;

    public static final short APP_CONNECTED = 50;

    public static final short UPDATE_FACE = 51;

    //------------------------------------------

    //web sockets
    public static final short WEB_SOCKETS = 52;

    public static final short EVENTOR = 53;
    public static final short WEB_HOOKS = 54;

    public static final short CREATE_APP = 55;
    public static final short UPDATE_APP = 56;
    public static final short DELETE_APP = 57;
    public static final short GET_PROJECT_BY_TOKEN = 58;
    public static final short EMAIL_QR = 59;
    public static final short GET_ENHANCED_GRAPH_DATA = 60;
    public static final short DELETE_ENHANCED_GRAPH_DATA = 61;

    public static final short GET_CLONE_CODE = 62;
    public static final short GET_PROJECT_BY_CLONE_CODE = 63;

    public static final short HARDWARE_LOG_EVENT = 64;
    public static final short HARDWARE_RESEND_FROM_BLUETOOTH = 65;
    public static final short LOGOUT = 66;

    public static final short CREATE_TILE_TEMPLATE = 67;
    public static final short UPDATE_TILE_TEMPLATE = 68;
    public static final short DELETE_TILE_TEMPLATE = 69;
    public static final short GET_WIDGET = 70;
    public static final short DEVICE_OFFLINE = 71;

    //http codes. Used only for stats
    public static final short HTTP_IS_HARDWARE_CONNECTED = 82;
    public static final short HTTP_IS_APP_CONNECTED = 83;
    public static final short HTTP_GET_PIN_DATA = 84;
    public static final short HTTP_UPDATE_PIN_DATA = 85;
    public static final short HTTP_NOTIFY = 86;
    public static final short HTTP_EMAIL = 87;
    public static final short HTTP_GET_PROJECT = 88;
    public static final short HTTP_QR = 89;
    public static final short HTTP_GET_HISTORY_DATA = 90;
    public static final short HTTP_START_OTA = 91;
    public static final short HTTP_STOP_OTA = 92;
    public static final short HTTP_CLONE = 93;
    public static final short HTTP_TOTAL = 94;

    private Command() {
    }

    //all this code just to make logging more user-friendly
    public static final Map<Short, String> VALUES_NAME = Collections.unmodifiableMap(new HashMap<Short, String>() {
            {
                put(RESPONSE, "Response");
                put(REDEEM, "Redeem");
                put(HARDWARE_CONNECTED, "HardwareConnected");
                put(REGISTER, "Register");
                put(LOGIN, "Login");
                put(LOGOUT, "Logout");
                put(LOAD_PROFILE_GZIPPED, "LoadProfile");
                put(APP_SYNC, "AppSync");
                put(SHARING, "Sharing");
                put(GET_TOKEN, "GetToken");
                put(ASSIGN_TOKEN, "AssignToken");
                put(PING, "Ping");
                put(SMS, "Sms");
                put(ACTIVATE_DASHBOARD, "Activate");
                put(DEACTIVATE_DASHBOARD, "Deactivate");
                put(REFRESH_TOKEN, "RefreshToken");
                put(GET_GRAPH_DATA, "GetGraphDataRequest");
                put(GET_ENHANCED_GRAPH_DATA, "GetEnhancedGraphDataRequest");
                put(DELETE_ENHANCED_GRAPH_DATA, "DeleteEnhancedGraphDataRequest");
                put(GET_GRAPH_DATA_RESPONSE, "GetGraphDataResponse");
                put(EXPORT_GRAPH_DATA, "ExportGraphData");
                put(SET_WIDGET_PROPERTY, "setWidgetProperty");
                put(BRIDGE, "Bridge");
                put(HARDWARE, "Hardware");
                put(GET_SHARE_TOKEN, "GetShareToken");
                put(REFRESH_SHARE_TOKEN, "RefreshShareToken");
                put(SHARE_LOGIN, "ShareLogin");
                put(CREATE_DASH, "CreateProject");
                put(UPDATE_DASH, "UpdateProject");
                put(DELETE_DASH, "DeleteProject");
                put(HARDWARE_SYNC, "HardwareSync");
                put(BLYNK_INTERNAL, "Internal");
                put(ADD_PUSH_TOKEN, "AddPushToken");

                put(TWEET, "Tweet");
                put(EMAIL, "Email");
                put(PUSH_NOTIFICATION, "Push");

                put(CREATE_WIDGET, "CreateWidget");
                put(UPDATE_WIDGET, "UpdateWidget");
                put(DELETE_WIDGET, "DeleteWidget");
                put(GET_WIDGET, "GetWidget");

                put(CREATE_TILE_TEMPLATE, "CreateTileTemplate");
                put(UPDATE_TILE_TEMPLATE, "UpdateTileTemplate");
                put(DELETE_TILE_TEMPLATE, "DeleteTileTemplate");

                put(CREATE_DEVICE, "CreateDevice");
                put(UPDATE_DEVICE, "UpdateDevice");
                put(DELETE_DEVICE, "DeleteDevice");
                put(GET_DEVICES, "GetDevices");

                put(ADD_ENERGY, "AddEnergy");
                put(GET_ENERGY, "GetEnergy");

                put(UPDATE_PROJECT_SETTINGS, "UpdateProjectSettings");

                put(GET_SERVER, "GetServer");
                put(CONNECT_REDIRECT, "ConnectRedirect");

                put(CREATE_APP, "CreateApp");
                put(UPDATE_APP, "UpdateApp");
                put(DELETE_APP, "DeleteApp");
                put(GET_PROJECT_BY_TOKEN, "GetProjectByToken");
                put(EMAIL_QR, "MailQRs");
                put(UPDATE_FACE, "UpdateFace");

                put(HARDWARE_LOG_EVENT, "HardwareLogEvent");
                put(HARDWARE_RESEND_FROM_BLUETOOTH, "HardwareResendFromBluetooth");

                put(GET_CLONE_CODE, "GetCloneCode");
                put(GET_PROJECT_BY_CLONE_CODE, "GetProjectByCloneCode");

                put(DEVICE_OFFLINE, "deviceOffline");

                put(HTTP_IS_HARDWARE_CONNECTED, "HttpIsHardwareConnected");
                put(HTTP_IS_APP_CONNECTED, "HttpIsAppConnected");
                put(HTTP_GET_PIN_DATA, "HttpGetPinData");
                put(HTTP_UPDATE_PIN_DATA, "HttpUpdatePinData");
                put(HTTP_NOTIFY, "HttpNotify");
                put(HTTP_EMAIL, "HttpEmail");
                put(HTTP_GET_PROJECT, "HttpGetProject");
                put(HTTP_QR, "QR");
                put(HTTP_CLONE, "Clone");
                put(HTTP_GET_HISTORY_DATA, "HttpGetHistoryData");
                put(HTTP_START_OTA, "HttpStartOTA");
                put(HTTP_TOTAL, "HttpTotal");

                put(WEB_SOCKETS, "WebSockets");

                put(EVENTOR, "Eventor");
                put(WEB_HOOKS, "WebHooks");
            }
    });

    public static String getNameByValue(short val) {
        return VALUES_NAME.get(val);
    }
    //--------------------------------------------------------

}

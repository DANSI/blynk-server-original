package cc.blynk.server.core.protocol.enums;

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
    public static final short DEVICE_CONNECTED = 4;

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
    public static final short HARDWARE_INFO = 17;
    public static final short HARDWARE = 20;
    //app commands
    public static final short CREATE_DASH = 21;
    public static final short SAVE_DASH = 22;
    public static final short DELETE_DASH = 23;
    public static final short LOAD_PROFILE_GZIPPED = 24;
    public static final short SYNC = 25;
    public static final short SHARING = 26;
    public static final short ADD_PUSH_TOKEN = 27;
    //app sharing commands
    public static final short GET_SHARED_DASH = 29;
    public static final short GET_SHARE_TOKEN = 30;
    public static final short REFRESH_SHARE_TOKEN = 31;
    public static final short SHARE_LOGIN = 32;
    //app commands
    public static final short CREATE_WIDGET = 33;
    public static final short UPDATE_WIDGET = 34;
    public static final short DELETE_WIDGET = 35;
    //------------------------------------------

    //http codes. Used only for stats
    public static final short HTTP_GET_PIN_DATA = 44;
    public static final short HTTP_UPDATE_PIN_DATA = 45;
    public static final short HTTP_NOTIFY = 46;
    public static final short HTTP_EMAIL = 47;
    public static final short HTTP_GET_PROJECT = 48;
    public static final short HTTP_TOTAL = 49;


    //all this code just to make logging more user-friendly
    public static final Map<Short, String> valuesName = new HashMap<Short, String>() {
            {
                put(RESPONSE, "Response");
                put(REDEEM, "Redeem");
                put(DEVICE_CONNECTED, "DeviceConnected");
                put(REGISTER, "Register");
                put(LOGIN, "Login");
                put(LOAD_PROFILE_GZIPPED, "LoadProfile");
                put(SYNC, "Sync");
                put(SHARING, "Sharing");
                put(GET_TOKEN, "GetToken");
                put(PING, "Ping");
                put(ACTIVATE_DASHBOARD, "Activate");
                put(DEACTIVATE_DASHBOARD, "Deactivate");
                put(REFRESH_TOKEN, "RefreshToken");
                put(GET_GRAPH_DATA, "GetGraphDataRequest");
                put(GET_GRAPH_DATA_RESPONSE, "GetGraphDataResponse");
                put(TWEET, "Tweet");
                put(EMAIL, "Email");
                put(PUSH_NOTIFICATION, "Push");
                put(BRIDGE, "Bridge");
                put(HARDWARE, "Hardware");
                put(GET_SHARED_DASH, "GetSharedDash");
                put(GET_SHARE_TOKEN, "GetShareToken");
                put(REFRESH_SHARE_TOKEN, "RefreshShareToken");
                put(SHARE_LOGIN, "ShareLogin");
                put(CREATE_DASH, "CreateProject");
                put(SAVE_DASH, "SaveProject");
                put(DELETE_DASH, "DeleteProject");
                put(HARDWARE_SYNC, "HardwareSync");
                put(HARDWARE_INFO, "HardwareInfo");
                put(ADD_PUSH_TOKEN, "AddPushToken");

                put(CREATE_WIDGET, "CreateWidget");
                put(UPDATE_WIDGET, "UpdateWidget");
                put(DELETE_WIDGET, "DeleteWidget");

                put(HTTP_GET_PIN_DATA, "HttpGetPinData");
                put(HTTP_UPDATE_PIN_DATA, "HttpUpdatePinData");
                put(HTTP_NOTIFY, "HttpNotify");
                put(HTTP_EMAIL, "HttpEmail");
                put(HTTP_GET_PROJECT, "HttpGetProject");
                put(HTTP_TOTAL, "HttpTotal");
            }
    };

    public static String getNameByValue(short val) {
        return valuesName.get(val);
    }
    //--------------------------------------------------------

}

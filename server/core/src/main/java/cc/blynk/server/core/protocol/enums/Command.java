package cc.blynk.server.core.protocol.enums;

import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.*;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.*;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.protocol.model.messages.common.PingMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.*;

import java.util.HashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public final class Command {

    public static final short RESPONSE = 0;

    //mobile client command
    public static final short REGISTER = 1;
    public static final short LOGIN = 2;
    public static final short SAVE_PROFILE = 3;

    public static final short GET_TOKEN = 5;
    public static final short PING = 6;
    public static final short ACTIVATE_DASHBOARD = 7;
    public static final short DEACTIVATE_DASHBOARD = 8;
    public static final short REFRESH_TOKEN = 9;
    public static final short GET_GRAPH_DATA = 10;
    public static final short GET_GRAPH_DATA_RESPONSE = 11;

    public static final short CREATE_DASH = 21;
    public static final short SAVE_DASH = 22;
    public static final short DELETE_DASH = 23;
    public static final short LOAD_PROFILE_GZIPPED = 24;
    public static final short SYNC = 25;
    public static final short SHARING = 26;
    public static final short ADD_PUSH_TOKEN = 27;

    //sharing commands
    public static final short GET_SHARED_DASH = 29;
    public static final short GET_SHARE_TOKEN = 30;
    public static final short REFRESH_SHARE_TOKEN = 31;
    public static final short SHARE_LOGIN = 32;
    //------------------------------------------

    //HARDWARE commands
    public static final short TWEET = 12;
    public static final short EMAIL = 13;
    public static final short PUSH_NOTIFICATION = 14;
    public static final short BRIDGE = 15;
    public static final short HARDWARE_SYNC = 16;
    public static final short HARDWARE_INFO = 17;
    public static final short HARDWARE = 20;
    //------------------------------------------

    //http codes. Used oonly for stats
    public static final short HTTP_GET_PIN_DATA = 35;
    public static final short HTTP_UPDATE_PIN_DATA = 36;
    public static final short HTTP_NOTIFY = 37;
    public static final short HTTP_EMAIL = 38;
    public static final short HTTP_GET_PROJECT = 39;
    public static final short HTTP_TOTAL = 40;


    //all this code just to make logging more user-friendly
    public static final Map<Short, String> valuesName = new HashMap<Short, String>() {
            {
                put(RESPONSE, ResponseMessage.class.getSimpleName());
                put(REGISTER, RegisterMessage.class.getSimpleName());
                put(LOGIN, LoginMessage.class.getSimpleName());
                put(SAVE_PROFILE, SaveProfileMessage.class.getSimpleName());
                put(LOAD_PROFILE_GZIPPED, LoadProfileGzippedBinaryMessage.class.getSimpleName());
                put(SYNC, SyncMessage.class.getSimpleName());
                put(SHARING, SharingMessage.class.getSimpleName());
                put(GET_TOKEN, GetTokenMessage.class.getSimpleName());
                put(PING, PingMessage.class.getSimpleName());
                put(ACTIVATE_DASHBOARD, ActivateDashboardMessage.class.getSimpleName());
                put(DEACTIVATE_DASHBOARD, DeActivateDashboardMessage.class.getSimpleName());
                put(REFRESH_TOKEN, RefreshTokenMessage.class.getSimpleName());
                put(GET_GRAPH_DATA, GetGraphDataStringMessage.class.getSimpleName());
                put(GET_GRAPH_DATA_RESPONSE, GetGraphDataBinaryMessage.class.getSimpleName());
                put(TWEET, TweetMessage.class.getSimpleName());
                put(EMAIL, MailMessage.class.getSimpleName());
                put(PUSH_NOTIFICATION, PushMessage.class.getSimpleName());
                put(BRIDGE, BridgeMessage.class.getSimpleName());
                put(HARDWARE, HardwareMessage.class.getSimpleName());
                put(GET_SHARED_DASH, GetSharedDashMessage.class.getSimpleName());
                put(GET_SHARE_TOKEN, GetShareTokenMessage.class.getSimpleName());
                put(REFRESH_SHARE_TOKEN, RefreshShareTokenMessage.class.getSimpleName());
                put(SHARE_LOGIN, ShareLoginMessage.class.getSimpleName());
                put(CREATE_DASH, CreateDashMessage.class.getSimpleName());
                put(SAVE_DASH, SaveDashMessage.class.getSimpleName());
                put(DELETE_DASH, DeleteDashMessage.class.getSimpleName());
                put(HARDWARE_SYNC, HardwareSyncMessage.class.getSimpleName());
                put(HARDWARE_INFO, HardwareInfoMessage.class.getSimpleName());
                put(ADD_PUSH_TOKEN, AddPushToken.class.getSimpleName());
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

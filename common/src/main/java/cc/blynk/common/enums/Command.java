package cc.blynk.common.enums;

import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.model.messages.protocol.BridgeMessage;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.model.messages.protocol.PingMessage;
import cc.blynk.common.model.messages.protocol.appllication.*;
import cc.blynk.common.model.messages.protocol.hardware.MailMessage;
import cc.blynk.common.model.messages.protocol.hardware.PushMessage;
import cc.blynk.common.model.messages.protocol.hardware.TweetMessage;

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
    public static final short LOAD_PROFILE = 4;
    public static final short GET_TOKEN = 5;
    public static final short PING = 6;
    public static final short ACTIVATE_DASHBOARD = 7;
    public static final short DEACTIVATE_DASHBOARD = 8;
    public static final short REFRESH_TOKEN = 9;
    public static final short GET_GRAPH_DATA = 10;
    public static final short GET_GRAPH_DATA_RESPONSE = 11;

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
    public static final short HARDWARE = 20;
    //------------------------------------------

    //all this code just to make logging more user-friendly
    public static final Map<Short, String> valuesName = new HashMap<Short, String>() {
            {
                put(RESPONSE, ResponseMessage.class.getSimpleName());
                put(REGISTER, RegisterMessage.class.getSimpleName());
                put(LOGIN, LoginMessage.class.getSimpleName());
                put(SAVE_PROFILE, SaveProfileMessage.class.getSimpleName());
                put(LOAD_PROFILE, LoadProfileMessage.class.getSimpleName());
                put(GET_TOKEN, GetTokenMessage.class.getSimpleName());
                put(PING, PingMessage.class.getSimpleName());
                put(ACTIVATE_DASHBOARD, ActivateDashboardMessage.class.getSimpleName());
                put(DEACTIVATE_DASHBOARD, DeActivateDashboardMessage.class.getSimpleName());
                put(REFRESH_TOKEN, RefreshTokenMessage.class.getSimpleName());
                put(GET_GRAPH_DATA, GetGraphDataMessage.class.getSimpleName());
                put(GET_GRAPH_DATA_RESPONSE, GetGraphDataResponseMessage.class.getSimpleName());
                put(TWEET, TweetMessage.class.getSimpleName());
                put(EMAIL, MailMessage.class.getSimpleName());
                put(PUSH_NOTIFICATION, PushMessage.class.getSimpleName());
                put(BRIDGE, BridgeMessage.class.getSimpleName());
                put(HARDWARE, HardwareMessage.class.getSimpleName());
            }
    };

    public static String getNameByValue(short val) {
        return valuesName.get(val);
    }
    //--------------------------------------------------------

}

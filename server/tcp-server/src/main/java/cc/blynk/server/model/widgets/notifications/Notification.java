package cc.blynk.server.model.widgets.notifications;

import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.model.widgets.Widget;

import java.util.HashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Notification extends Widget {

    private static final int MAX_PUSH_BODY_SIZE = 255;

    public Map<String, String> androidTokens = new HashMap<>();

    public Map<String, String> iOSTokens = new HashMap<>();

    //todo remove this field when ready
    public String token;
    //todo remove this field when ready
    public String iOSToken;

    public boolean notifyWhenOffline;

    public Priority priority = Priority.normal;

    public static boolean isWrongBody(String body) {
        return body == null || body.equals("") || body.length() > MAX_PUSH_BODY_SIZE;
    }

    public void cleanPrivateData() {
        token = null;
        iOSToken = null;
    }

    @Override
    public void updateIfSame(HardwareBody body) {

    }

    @Override
    public boolean isSame(byte pin, PinType type) {
        return false;
    }

    @Override
    public String getValue(byte pin, PinType type) {
        return null;
    }

    @Override
    public String getJsonValue() {
        return null;
    }

    @Override
    public String makeHardwareBody() {
        return null;
    }

    public boolean hasNoToken() {
        return (token == null || token.equals("")) &&
               (iOSToken == null || iOSToken.equals("")) &&
               (iOSTokens.size() == 0 && androidTokens.size() == 0);
    }
}

package cc.blynk.server.core.model.widgets.notifications;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.notifications.push.enums.Priority;

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

    public boolean notifyWhenOffline;

    public int notifyWhenOfflineIgnorePeriod;

    public Priority priority = Priority.normal;

    public static boolean isWrongBody(String body) {
        return body == null || body.equals("") || body.length() > MAX_PUSH_BODY_SIZE;
    }

    public void cleanPrivateData() {
        androidTokens = new HashMap<>();
        iOSTokens = new HashMap<>();
    }

    @Override
    public void updateIfSame(byte pin, PinType type, String[] values) {

    }

    @Override
    public boolean isSame(byte pin, PinType type) {
        return false;
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
        return iOSTokens.size() == 0 && androidTokens.size() == 0;
    }

    @Override
    public String getModeType() {
        return null;
    }

    @Override
    public int getPrice() {
        return 400;
    }
}

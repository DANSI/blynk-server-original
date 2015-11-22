package cc.blynk.server.model.widgets.notifications;

import cc.blynk.server.model.widgets.Widget;

import java.util.HashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Notification extends Widget {

    public Map<String, String> androidTokens = new HashMap<>();

    public Map<String, String> iOSTokens = new HashMap<>();

    public String token;

    public String iOSToken;

    public boolean notifyWhenOffline;

    public Priority priority = Priority.normal;

    public void cleanPrivateData() {
        token = null;
        iOSToken = null;
    }

}

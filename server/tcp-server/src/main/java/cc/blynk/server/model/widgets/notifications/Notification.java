package cc.blynk.server.model.widgets.notifications;

import cc.blynk.server.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Notification extends Widget {

    public String token;

    public String iOSToken;

    public boolean notifyWhenOffline;

    public Priority priority = Priority.normal;

    public void cleanPrivateData() {
        token = null;
        iOSToken = null;
    }

}

package cc.blynk.server.model.widgets.others;

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

    public void cleanPrivateData() {
        token = null;
        iOSToken = null;
    }

}

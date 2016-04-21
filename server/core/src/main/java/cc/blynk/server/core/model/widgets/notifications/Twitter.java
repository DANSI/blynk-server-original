package cc.blynk.server.core.model.widgets.notifications;

import cc.blynk.server.core.model.widgets.NoPinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Twitter extends NoPinWidget {

    public String token;

    public String secret;

    public String username;

    public void cleanPrivateData() {
        token = null;
        secret = null;
        username = null;
    }

    @Override
    public int getPrice() {
        return 0;
    }
}

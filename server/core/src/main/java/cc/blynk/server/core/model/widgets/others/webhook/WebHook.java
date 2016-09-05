package cc.blynk.server.core.model.widgets.others.webhook;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.09.16.
 */
public class WebHook extends OnePinWidget {

    public String url;

    public String method;

    public Header[] headers;

    public String body;

    @Override
    public String getModeType() {
        return null;
    }

    @Override
    public int getPrice() {
        return 500;
    }
}

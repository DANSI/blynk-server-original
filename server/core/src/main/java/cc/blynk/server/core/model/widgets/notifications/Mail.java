package cc.blynk.server.core.model.widgets.notifications;

import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.utils.http.ContentType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Mail extends NoPinWidget {

    public volatile String to;

    public volatile ContentType contentType = ContentType.TEXT_HTML;

    @Override
    public void updateValue(Widget oldWidget) {
        if (oldWidget instanceof Mail) {
            Mail oldMailWidget = (Mail) oldWidget;
            this.to = oldMailWidget.to;
            this.contentType = oldMailWidget.contentType;
        }
    }

    @Override
    public void erase() {
        this.to = null;
    }

    @Override
    public int getPrice() {
        return 100;
    }

    public boolean isText() {
        return contentType == ContentType.TEXT_PLAIN;
    }

}

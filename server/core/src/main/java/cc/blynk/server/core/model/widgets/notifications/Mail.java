package cc.blynk.server.core.model.widgets.notifications;

import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.utils.http.ContentType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Mail extends NoPinWidget {

    public String to;

    public ContentType contentType = ContentType.TEXT_HTML;

    @Override
    public int getPrice() {
        return 100;
    }

    public boolean isText() {
        return contentType == ContentType.TEXT_PLAIN;
    }

}

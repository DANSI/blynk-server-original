package cc.blynk.server.core.model.widgets.others.webhook;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import io.netty.channel.Channel;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.09.16.
 */
public class WebHook extends OnePinWidget {

    public String url;

    //GET is always default so we don't do null checks
    public SupportedWebhookMethod method = SupportedWebhookMethod.GET;

    public Header[] headers;

    public String body;

    public boolean isValid() {
        return url != null && !url.equals("");
    }

    //a bit ugly but as quick fix ok
    public boolean isSameWebHook(byte pin, PinType type) {
        return super.isSame(pin, type);
    }

    @Override
    public boolean isSame(byte pin, PinType type) {
        return false;
    }

    @Override
    public boolean updateIfSame(byte pin, PinType type, String value) {
        return false;
    }

    @Override
    public void sendSyncOnActivate(Channel appChannel, int dashId) {
    }

    @Override
    public String getModeType() {
        return null;
    }

    @Override
    public int getPrice() {
        return 500;
    }
}

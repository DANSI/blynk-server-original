package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Video extends OnePinWidget {

    public String url;

    public boolean forceTCP;

    @Override
    public void sendAppSync(Channel appChannel, int dashId, int targetId) {
    }

    @Override
    public void sendHardSync(ChannelHandlerContext ctx, int msgId, int deviceId) {
    }

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case URL :
                this.url = propertyValue;
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }

    @Override
    //supports only virtual pins
    public PinMode getModeType() {
        return null;
    }

    @Override
    public int getPrice() {
        return 500;
    }

}

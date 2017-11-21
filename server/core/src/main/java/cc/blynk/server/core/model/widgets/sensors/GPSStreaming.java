package cc.blynk.server.core.model.widgets.sensors;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class GPSStreaming extends OnePinWidget {

    public int accuracy;

    private int frequency;

    private long interval;

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public void sendHardSync(ChannelHandlerContext ctx, int msgId, int deviceId) {
    }

    @Override
    public int getPrice() {
        return 500;
    }
}

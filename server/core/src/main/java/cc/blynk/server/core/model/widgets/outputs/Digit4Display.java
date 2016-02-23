package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.controls.HardwareSyncWidget;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Digit4Display extends OnePinWidget implements FrequencyWidget, HardwareSyncWidget {

    private int frequency;

    private transient long lastRequestTS;

    @Override
    public final int getFrequency() {
        return frequency;
    }

    @Override
    public final long getLastRequestTS(String body) {
        return lastRequestTS;
    }

    @Override
    public final void setLastRequestTS(String body, long now) {
        this.lastRequestTS = now;
    }

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        final String body = makeHardwareBody();
        if (body != null) {
            ctx.write(new HardwareMessage(msgId, body));
        }
    }

}

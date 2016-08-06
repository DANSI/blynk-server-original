package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.controls.HardwareSyncWidget;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Gauge extends OnePinWidget implements FrequencyWidget, HardwareSyncWidget {

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
            ctx.write(makeStringMessage(HARDWARE, msgId, body), ctx.voidPromise());
        }
    }

    @Override
    public int getPrice() {
        return 300;
    }
}

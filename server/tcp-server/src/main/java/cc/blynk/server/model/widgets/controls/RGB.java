package cc.blynk.server.model.widgets.controls;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.server.model.Pin;
import cc.blynk.server.model.widgets.MultiPinWidget;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class RGB extends MultiPinWidget implements SyncWidget {

    public boolean splitMode;

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        if (pins == null) {
            return;
        }
        if (splitMode) {
            for (Pin pin : pins) {
                if (pin.pin != -1) {
                    ctx.write(new HardwareMessage(msgId, pin.makeHardwareBody()));
                }
            }
        } else {
            if (pins[0].pin != -1) {
                ctx.write(new HardwareMessage(msgId, makeHardwareBody()));
            }
        }
    }

}

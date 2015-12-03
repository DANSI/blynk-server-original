package cc.blynk.server.model.widgets.controls;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.server.model.Pin;
import cc.blynk.server.model.widgets.MultiPinWidget;
import cc.blynk.server.model.widgets.OnePinWidget;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class TwoAxisJoystick extends MultiPinWidget implements SyncWidget {

    public boolean split;

    public boolean autoReturnOn;

    public boolean portraitLocked;

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        if (pins == null) {
            return;
        }
        if (split) {
            for (Pin pin : pins) {
                ctx.write(new HardwareMessage(msgId, OnePinWidget.makeHardwareBody(pin)));
            }
        } else {
            ctx.write(new HardwareMessage(msgId, makeHardwareBodyMerge()));
        }
    }

}

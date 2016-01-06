package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class OneAxisJoystick extends OnePinWidget implements SyncWidget {

    public boolean autoReturn;

    public boolean horizontal;

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        if (pin != -1) {
            ctx.write(new HardwareMessage(msgId, makeHardwareBody()));
        }
    }

    @Override
    public String getModeType() {
        return "out";
    }
}

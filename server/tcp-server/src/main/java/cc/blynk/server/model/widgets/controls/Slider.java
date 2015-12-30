package cc.blynk.server.model.widgets.controls;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.server.model.widgets.OnePinWidget;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Slider extends OnePinWidget implements SyncWidget {

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        if (pin != -1) {
            ctx.write(new HardwareMessage(msgId, makeHardwareBody()));
        }
    }
}

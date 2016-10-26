package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.OnePinWidget;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.BlynkByteBufUtil.makeStringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Button extends OnePinWidget implements HardwareSyncWidget {

    public boolean pushMode;

    public String onLabel;

    public String offLabel;

    public boolean invertedOn = false;

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        String body = makeHardwareBody();
        if (body != null) {
            ctx.write(makeStringMessage(HARDWARE, msgId, body), ctx.voidPromise());
        }
    }

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 200;
    }
}

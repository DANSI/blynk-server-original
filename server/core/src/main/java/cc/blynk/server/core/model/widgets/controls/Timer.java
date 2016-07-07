package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.OnePinWidget;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Timer extends OnePinWidget implements HardwareSyncWidget {

    public long startTime = -1;

    public String startValue;

    public long stopTime = -1;

    public String stopValue;

    public boolean invertedOn = false;

    //this trick called field hiding.
    //it used to avoid volatile in OnePinWidget
    //so timer has it's own field "value"
    //that could be changed via other thread - TimerWorker
    //https://github.com/blynkkk/blynk-server/issues/208
    public volatile String value;

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        if (value != null) {
            ctx.write(makeStringMessage(HARDWARE, msgId, value), ctx.voidPromise());
        }
    }

    @Override
    public String makeHardwareBody() {
        if (pin == -1 || value == null || pinType == null) {
            return null;
        }
        return value;
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

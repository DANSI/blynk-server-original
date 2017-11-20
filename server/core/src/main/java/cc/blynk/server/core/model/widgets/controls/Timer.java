package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Timer extends OnePinWidget {

    public int startTime = -1;

    public String startValue;

    public int stopTime = -1;

    public String stopValue;

    public boolean isValidStart() {
        return isValidTime(startTime) && isValidValue(startValue);
    }

    public boolean isValidStop() {
        return isValidTime(stopTime) && isValidValue(stopValue);
    }

    private static boolean isValidTime(int time) {
        return time > -1 && time < 86400;
    }

    private static boolean isValidValue(String value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public void sendHardSync(ChannelHandlerContext ctx, int msgId, int deviceId) {
        if (value != null && this.deviceId == deviceId) {
            ctx.write(makeUTF8StringMessage(HARDWARE, msgId, value), ctx.voidPromise());
        }
    }

    @Override
    public String makeHardwareBody() {
        if (isNotValid() || value == null) {
            return null;
        }
        return value;
    }

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 200;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Timer)) {
            return false;
        }

        Timer timer = (Timer) o;

        return id == timer.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}

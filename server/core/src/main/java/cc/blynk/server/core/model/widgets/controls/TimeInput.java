package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.OnePinWidget;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.08.16.
 */
public class TimeInput extends OnePinWidget implements HardwareSyncWidget {

    public String format;

    public int[] days;

    public int startAt;

    public int stopAt;

    public String timezone;

    public String timezoneName;

    public boolean isStartStopAllowed;

    public boolean isDayOfWeekAllowed;

    public boolean isSunsetSunriseAllowed;

    public boolean isTimezoneAllowed;

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

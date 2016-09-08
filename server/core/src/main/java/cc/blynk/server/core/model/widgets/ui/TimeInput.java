package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.controls.HardwareSyncWidget;
import cc.blynk.server.core.model.widgets.others.rtc.StringToZoneId;
import cc.blynk.server.core.model.widgets.others.rtc.ZoneIdToString;
import cc.blynk.utils.ParseUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.netty.channel.ChannelHandlerContext;

import java.time.ZoneId;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.08.16.
 */
public class TimeInput extends OnePinWidget implements HardwareSyncWidget {

    public String format;

    public int[] days;

    public int startAt = -1;

    public int stopAt = -1;

    @JsonSerialize(using = ZoneIdToString.class)
    @JsonDeserialize(using = StringToZoneId.class, as = ZoneId.class)
    public ZoneId tzName;

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
    public boolean updateIfSame(byte pin, PinType type, String value) {
        if (super.updateIfSame(pin, type, value)) {
            String[] values = value.split(BODY_SEPARATOR_STRING);
            startAt = "".equals(values[0]) ? -1 : ParseUtil.parseInt(values[0]);
            stopAt = "".equals(values[1]) ? -1 : ParseUtil.parseInt(values[1]);
            tzName = ZoneId.of(values[2]);
            if (values.length == 3) {
                days = null;
            } else {
                String[] daysString = values[3].split(",");
                days = new int[daysString.length];
                for (int i = 0; i < daysString.length; i++) {
                    days[i] = ParseUtil.parseInt(daysString[i]);
                }
            }
            return true;
        }
        return false;
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

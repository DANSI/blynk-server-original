package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.utils.LimitedQueue;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;

import java.util.List;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Terminal extends OnePinWidget {

    public boolean autoScrollOn;

    public boolean terminalInputOn;

    public boolean textLightOn;

    //configured property via server.properties
    private static final int POOL_SIZE = ParseUtil.parseInt(System.getProperty("terminal.strings.pool.size", "25"));

    public transient final List<String> lastCommands = new LimitedQueue<>(POOL_SIZE);

    @Override
    public boolean updateIfSame(byte pin, PinType type, String value) {
        if (isSame(pin, type)) {
            this.value = value;
            this.lastCommands.add(value);
            return true;
        }
        return false;
    }

    @Override
    public void sendSyncOnActivate(Channel appChannel, int dashId) {
        if (pin == -1 || pinType == null || lastCommands.size() == 0) {
            return;
        }
        for (String storedValue : lastCommands) {
            String body = makeHardwareBody(pinType, pin, storedValue);
            appChannel.write(makeStringMessage(SYNC, 1111, dashId + StringUtils.BODY_SEPARATOR_STRING + body));
        }
    }

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public int getPrice() {
        return 200;
    }
}

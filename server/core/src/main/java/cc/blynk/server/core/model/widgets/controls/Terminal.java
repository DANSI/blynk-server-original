package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.structure.LimitedArrayDeque;
import io.netty.channel.Channel;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.utils.BlynkByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.makeBody;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Terminal extends OnePinWidget {

    private static final int POOL_SIZE = ParseUtil.parseInt(System.getProperty("terminal.strings.pool.size", "25"));
    private transient final LimitedArrayDeque<String> lastCommands = new LimitedArrayDeque<>(POOL_SIZE);

    public boolean autoScrollOn;

    public boolean terminalInputOn;

    public boolean textLightOn;

    @Override
    public boolean updateIfSame(int deviceId, byte pin, PinType type, String value) {
        if (isSame(deviceId, pin, type)) {
            this.value = value;
            this.lastCommands.add(value);
            return true;
        }
        return false;
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId) {
        if (pin == -1 || pinType == null || lastCommands.size() == 0) {
            return;
        }
        for (String storedValue : lastCommands) {
            String body = makeBody(dashId, deviceId, makeHardwareBody(pinType, pin, storedValue));
            appChannel.write(makeUTF8StringMessage(APP_SYNC, 1111, body));
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

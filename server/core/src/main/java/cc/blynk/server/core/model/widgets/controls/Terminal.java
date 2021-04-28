package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.storage.value.MultiPinStorageValue;
import cc.blynk.server.core.model.storage.value.MultiPinStorageValueType;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.utils.structure.LimitedArrayDeque;
import cc.blynk.utils.structure.TerminalLimitedQueue;
import io.netty.channel.Channel;

import java.util.Iterator;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Terminal extends OnePinWidget {

    //todo move to persistent LCDLimitedQueue?
    private transient final LimitedArrayDeque<String> lastCommands =
            new LimitedArrayDeque<>(TerminalLimitedQueue.POOL_SIZE);

    public boolean autoScrollOn;

    public boolean terminalInputOn;

    public boolean textLightOn;

    public boolean attachNewLine;

    @Override
    public boolean updateIfSame(int deviceId, short pin, PinType type, String value) {
        if (isSame(deviceId, pin, type)) {
            if ("clr".equals(value)) {
                this.lastCommands.clear();
            } else {
                this.lastCommands.add(value);
            }
            return true;
        }
        return false;
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, int targetId) {
        if (isNotValid() || lastCommands.size() == 0) {
            return;
        }
        if (targetId == ANY_TARGET || this.deviceId == targetId) {
            Iterator<String> valIterator = lastCommands.iterator();
            if (valIterator.hasNext()) {
                String body = makeMultiValueHardwareBody(dashId, deviceId, pinType.pintTypeChar, pin, valIterator);
                appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body));
            }
        }
    }

    @Override
    public String makeHardwareBody() {
        if (isNotValid() || lastCommands.size() == 0) {
            return null;
        }
        //terminal supports only virtual pins
        return makeHardwareBody(pinType, pin, lastCommands.getLast());
    }

    @Override
    public PinStorageValue getPinStorageValue() {
        return new MultiPinStorageValue(MultiPinStorageValueType.TERMINAL);
    }

    @Override
    public boolean isMultiValueWidget() {
        return true;
    }

    @Override
    public String getJsonValue() {
        return JsonParser.toJson(lastCommands);
    }

    @Override
    //terminal supports only virtual pins
    public PinMode getModeType() {
        return null;
    }

    @Override
    public int getPrice() {
        return 200;
    }

    @Override
    public void erase() {
        this.lastCommands.clear();
    }
}

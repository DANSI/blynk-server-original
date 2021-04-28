package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.storage.value.MultiPinStorageValue;
import cc.blynk.server.core.model.storage.value.MultiPinStorageValueType;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.server.core.model.widgets.FrequencyWidget;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.structure.LCDLimitedQueue;
import cc.blynk.utils.structure.LimitedArrayDeque;
import io.netty.channel.Channel;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LCD extends MultiPinWidget implements FrequencyWidget {

    public boolean advancedMode;

    public String textFormatLine1;
    public String textFormatLine2;

    public boolean textLight;

    private int frequency;

    private transient long lastRequestTS;

    //todo move to persistent LCDLimitedQueue?
    private transient final LimitedArrayDeque<String> lastCommands = new LimitedArrayDeque<>(LCDLimitedQueue.POOL_SIZE);

    private static void sendSyncOnActivate(DataStream dataStream, int dashId, int deviceId, Channel appChannel) {
        if (dataStream.notEmptyAndIsValid()) {
            String body = prependDashIdAndDeviceId(dashId, deviceId, dataStream.makeHardwareBody());
            appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body),
                    appChannel.voidPromise());
        }
    }

    @Override
    public boolean updateIfSame(int deviceId, short pinIn, PinType type, String value) {
        boolean isSame = false;
        if (dataStreams != null && this.deviceId == deviceId) {
            for (DataStream dataStream : dataStreams) {
                if (dataStream.isSame(pinIn, type)) {
                    dataStream.value = value;
                    isSame = true;
                }
            }
            if (advancedMode && isSame && value != null) {
                lastCommands.add(value);
            }
        }
        return isSame;
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, int targetId) {
        if (dataStreams == null) {
            return;
        }

        //do not send SYNC message for widgets assigned to device selector
        //as it will be duplicated later.
        if (isAssignedToDeviceSelector()) {
            return;
        }

        if (targetId == ANY_TARGET || this.deviceId == targetId) {
            if (advancedMode) {
                for (String command : lastCommands) {
                    dataStreams[0].value = command;
                    sendSyncOnActivate(dataStreams[0], dashId, deviceId, appChannel);
                }
            } else {
                for (DataStream dataStream : dataStreams) {
                    sendSyncOnActivate(dataStream, dashId, deviceId, appChannel);
                }
            }
        }
    }

    @Override
    public boolean isSplitMode() {
        return !advancedMode;
    }

    @Override
    public boolean isTicked(long now) {
        if (hasReadingInterval() && now >= lastRequestTS + frequency) {
            this.lastRequestTS = now;
            return true;
        }
        return false;
    }

    @Override
    public boolean hasReadingInterval() {
        return frequency > 0;
    }

    @Override
    public void writeReadingCommand(Channel channel) {
        if (dataStreams == null) {
            return;
        }
        for (DataStream dataStream : dataStreams) {
            if (dataStream.isValid()) {
                StringMessage msg = makeUTF8StringMessage(HARDWARE, READING_MSG_ID,
                        DataStream.makeReadingHardwareBody(dataStream.pinType.pintTypeChar, dataStream.pin));
                channel.write(msg, channel.voidPromise());
            }
        }
    }

    @Override
    public PinStorageValue getPinStorageValue() {
        return new MultiPinStorageValue(MultiPinStorageValueType.LCD);
    }

    @Override
    public boolean isMultiValueWidget() {
        return true;
    }

    @Override
    public int getDeviceId() {
        return deviceId;
    }

    @Override
    public PinMode getModeType() {
        return PinMode.in;
    }

    @Override
    public int getPrice() {
        return 400;
    }
}

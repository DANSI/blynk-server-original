package cc.blynk.server.core.model.storage;

import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.structure.BaseLimitedQueue;
import io.netty.channel.Channel;

import java.util.Collection;

import static cc.blynk.server.core.model.widgets.AppSyncWidget.SYNC_DEFAULT_MESSAGE_ID;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27/04/2018.
 *
 */
public class MultiPinStorageValue extends PinStorageValue {

    public final MultiPinStorageValueType type;

    public final BaseLimitedQueue<String> values;

    public MultiPinStorageValue(MultiPinStorageValueType multiPinStorageValueType) {
        this.type = multiPinStorageValueType;
        this.values = multiPinStorageValueType.getQueue();
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, PinStorageKey key) {
        if (values.size() > 0) {
            for (String value : values) {
                String body = key.makeHardwareBody(value);
                String finalBody = prependDashIdAndDeviceId(dashId, key.deviceId, body);
                //special case for setProperty
                short cmdType = key.getCmdType();
                StringMessage message = makeUTF8StringMessage(cmdType, SYNC_DEFAULT_MESSAGE_ID, finalBody);
                appChannel.write(message, appChannel.voidPromise());
            }
        }
    }

    @Override
    public Collection<String> values() {
        return values;
    }

    @Override
    public void update(String value) {
        values.add(value);
    }
}

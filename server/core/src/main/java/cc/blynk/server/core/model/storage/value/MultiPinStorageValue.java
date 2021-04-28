package cc.blynk.server.core.model.storage.value;

import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.structure.BaseLimitedQueue;
import io.netty.channel.Channel;

import java.util.Collection;
import java.util.Iterator;

import static cc.blynk.server.core.model.widgets.MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27/04/2018.
 */
public class MultiPinStorageValue extends PinStorageValue {

    public final MultiPinStorageValueType type;

    public final BaseLimitedQueue<String> values;

    public MultiPinStorageValue(MultiPinStorageValueType multiPinStorageValueType) {
        this.type = multiPinStorageValueType;
        this.values = multiPinStorageValueType.getQueue();
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, DashPinStorageKey key) {
        if (values.size() > 0) {
            Iterator<String> valIterator = values.iterator();
            if (valIterator.hasNext()) {
                String last = null;
                StringBuilder sb = new StringBuilder();
                sb.append(dashId).append(DEVICE_SEPARATOR).append(key.deviceId).append(BODY_SEPARATOR)
                        .append(key.pinTypeChar).append('m').append(BODY_SEPARATOR).append(key.pin);
                while (valIterator.hasNext()) {
                    last = valIterator.next();
                    sb.append(BODY_SEPARATOR).append(last);
                }

                appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, sb.toString()));

                //special case, when few widgets are on the same pin
                String body = prependDashIdAndDeviceId(dashId, key.deviceId, key.makeHardwareBody(last));
                StringMessage message = makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body);
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

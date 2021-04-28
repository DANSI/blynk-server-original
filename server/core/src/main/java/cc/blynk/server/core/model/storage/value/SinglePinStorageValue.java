package cc.blynk.server.core.model.storage.value;

import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import com.fasterxml.jackson.annotation.JsonValue;
import io.netty.channel.Channel;

import java.util.Collection;
import java.util.Collections;

import static cc.blynk.server.core.model.widgets.MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27/04/2018.
 *
 */
public class SinglePinStorageValue extends PinStorageValue {

    public volatile String value;

    public SinglePinStorageValue() {
    }

    public SinglePinStorageValue(String value) {
        this.value = value;
    }

    @Override
    public void update(String value) {
        this.value = value;
    }

    @Override
    public Collection<String> values() {
        if (value == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(value);
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, DashPinStorageKey key) {
        if (value != null) {
            String body = key.makeHardwareBody(value);
            String finalBody = prependDashIdAndDeviceId(key.dashId, key.deviceId, body);
            //special case for setProperty
            short cmdType = key.getCmdType();
            StringMessage message = makeUTF8StringMessage(cmdType, SYNC_DEFAULT_MESSAGE_ID, finalBody);
            appChannel.write(message, appChannel.voidPromise());
        }
    }

    @Override
    @JsonValue
    public String toString() {
        return value == null ? "" : value;
    }
}

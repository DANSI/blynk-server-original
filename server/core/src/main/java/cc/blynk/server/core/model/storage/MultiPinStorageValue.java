package cc.blynk.server.core.model.storage;

import cc.blynk.utils.structure.BaseLimitedQueue;

import java.util.Collection;

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
    public Collection<String> values() {
        return values;
    }

    @Override
    public void update(String value) {
        values.add(value);
    }
}

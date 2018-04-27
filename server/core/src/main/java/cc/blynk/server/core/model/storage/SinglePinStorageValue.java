package cc.blynk.server.core.model.storage;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collection;
import java.util.Collections;

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
    @JsonValue
    public String toString() {
        return value == null ? "" : value;
    }
}

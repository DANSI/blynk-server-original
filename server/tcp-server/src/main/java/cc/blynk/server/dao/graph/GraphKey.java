package cc.blynk.server.dao.graph;

import cc.blynk.server.model.enums.PinType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.15.
 */
public class GraphKey {

    public final int dashId;

    public final byte pin;

    public final PinType pinType;

    public GraphKey(int dashId, byte pin, PinType pinType) {
        this.dashId = dashId;
        this.pin = pin;
        this.pinType = pinType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphKey key = (GraphKey) o;

        if (dashId != key.dashId) return false;
        if (pin != key.pin) return false;
        return pinType == key.pinType;

    }

    @Override
    public int hashCode() {
        int result = dashId;
        result = 31 * result + (int) pin;
        result = 31 * result + (pinType != null ? pinType.hashCode() : 0);
        return result;
    }
}

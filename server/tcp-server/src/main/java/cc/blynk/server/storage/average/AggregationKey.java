package cc.blynk.server.storage.average;

import cc.blynk.server.model.enums.PinType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class AggregationKey {

    public String username;
    public int dashId;
    public PinType pinType;
    public byte pin;
    public long ts;

    public AggregationKey(String username, int dashId, PinType pinType, byte pin, long ts) {
        this.username = username;
        this.dashId = dashId;
        this.pinType = pinType;
        this.pin = pin;
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggregationKey that = (AggregationKey) o;

        if (dashId != that.dashId) return false;
        if (pin != that.pin) return false;
        if (ts != that.ts) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return pinType == that.pinType;

    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + dashId;
        result = 31 * result + (pinType != null ? pinType.hashCode() : 0);
        result = 31 * result + (int) pin;
        result = 31 * result + (int) (ts ^ (ts >>> 32));
        return result;
    }
}

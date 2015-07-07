package cc.blynk.server.dao.graph;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.15.
 */
public class GraphKey {

    public int dashId;

    public byte pin;

    public GraphKey(int dashId, byte pin) {
        this.dashId = dashId;
        this.pin = pin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphKey graphKey = (GraphKey) o;

        if (dashId != graphKey.dashId) return false;
        return pin == graphKey.pin;

    }

    @Override
    public int hashCode() {
        int result = dashId;
        result = 31 * result + (int) pin;
        return result;
    }
}

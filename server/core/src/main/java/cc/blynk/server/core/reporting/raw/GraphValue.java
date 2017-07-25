package cc.blynk.server.core.reporting.raw;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.07.17.
 */
public final class GraphValue {

    public final double value;

    public final long ts;

    public GraphValue(double value, long ts) {
        this.value = value;
        this.ts = ts;
    }
}

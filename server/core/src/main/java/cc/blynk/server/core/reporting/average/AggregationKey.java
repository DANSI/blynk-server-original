package cc.blynk.server.core.reporting.average;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.reporting.raw.BaseReportingKey;

import java.io.Serializable;
import java.util.Comparator;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public final class AggregationKey implements Serializable {

    public static final Comparator<AggregationKey> AGGREGATION_KEY_COMPARATOR = (o1, o2) -> (int) (o1.ts - o2.ts);

    private final BaseReportingKey baseReportingKey;
    public final long ts;

    public AggregationKey(String email, String appName, int dashId, int deviceId, PinType pinType, short pin, long ts) {
        this(new BaseReportingKey(email, appName, dashId, deviceId, pinType, pin), ts);
    }

    public AggregationKey(BaseReportingKey baseReportingKey, long ts) {
        this.baseReportingKey = baseReportingKey;
        this.ts = ts;
    }

    public long getTs(GraphGranularityType type) {
        return ts * type.period;
    }

    public boolean isOutdated(long nowTruncatedToPeriod) {
        return ts < nowTruncatedToPeriod;
    }

    public String getEmail() {
        return baseReportingKey.email;
    }

    public String getAppName() {
        return baseReportingKey.appName;
    }

    public int getDashId() {
        return baseReportingKey.dashId;
    }

    public int getDeviceId() {
        return baseReportingKey.deviceId;
    }

    public PinType getPinType() {
        return baseReportingKey.pinType;
    }

    public short getPin() {
        return baseReportingKey.pin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AggregationKey)) {
            return false;
        }

        AggregationKey that = (AggregationKey) o;

        if (ts != that.ts) {
            return false;
        }
        return baseReportingKey != null
                ? baseReportingKey.equals(that.baseReportingKey)
                : that.baseReportingKey == null;
    }

    @Override
    public int hashCode() {
        int result = baseReportingKey != null
                ? baseReportingKey.hashCode()
                : 0;
        result = 31 * result + (int) (ts ^ (ts >>> 32));
        return result;
    }
}

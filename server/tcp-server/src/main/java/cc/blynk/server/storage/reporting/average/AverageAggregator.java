package cc.blynk.server.storage.reporting.average;

import cc.blynk.server.model.enums.PinType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class AverageAggregator {

    public static final long HOURS = 1000 * 60 * 60;
    public static final long DAY = 24 * HOURS;

    private final ConcurrentHashMap<AggregationKey, AggregationValue> hourly = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AggregationKey, AggregationValue> daily = new ConcurrentHashMap<>();

    private static void aggregate(Map<AggregationKey, AggregationValue> map, AggregationKey key, double value) {
        AggregationValue aggregationValue = map.get(key);
        if (aggregationValue == null) {
            final AggregationValue aggregationValueTmp = new AggregationValue(value);
            aggregationValue = map.putIfAbsent(key, aggregationValueTmp);
            if (aggregationValue == null) {
                aggregationValue = aggregationValueTmp;
            }
        }

        aggregationValue.update(value);
    }

    public void collect(String username, int dashId, PinType pinType, byte pin, long ts, String value) {
        double val = Double.parseDouble(value);

        aggregateByHour(username, dashId, pinType, pin, ts, val);
        aggregateByDay(username, dashId, pinType, pin, ts, val);
    }

    private void aggregateByHour(String username, int dashId, PinType pinType, byte pin, long ts, double val) {
        aggregate(hourly, new AggregationKey(username, dashId, pinType, pin, ts / HOURS), val);
    }

    private void aggregateByDay(String username, int dashId, PinType pinType, byte pin, long ts, double val) {
        aggregate(daily, new AggregationKey(username, dashId, pinType, pin, ts / DAY), val);
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getHourly() {
        return hourly;
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getDaily() {
        return daily;
    }
}

package cc.blynk.server.storage.average;

import cc.blynk.server.model.enums.PinType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class AverageAggregator {

    public static final long HOURS = 1000 * 60 * 60;

    private final ConcurrentHashMap<AggregationKey, AggregationValue> map = new ConcurrentHashMap<>();

    public void collect(String username, int dashId, PinType pinType, byte pin, long ts, String value) {
        double val = Double.parseDouble(value);

        aggregateByHour(username, dashId, pinType, pin, ts, val);
    }

    private void aggregateByHour(String username, int dashId, PinType pinType, byte pin, long ts, double val) {
        aggregate(new AggregationKey(username, dashId, pinType, pin, ts / HOURS), val);
    }

    private void aggregate(AggregationKey key, double value) {
        AggregationValue aggregationValue = map.get(key);
        if (aggregationValue == null) {
            final AggregationValue aggregationValueTmp = new AggregationValue(value);
            aggregationValue = map.putIfAbsent(key, aggregationValueTmp);
            if (aggregationValue == null) {
                aggregationValue = aggregationValueTmp;
            }
        }

        //todo not threadsafe
        aggregationValue.update(value);
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getMap() {
        return map;
    }
}

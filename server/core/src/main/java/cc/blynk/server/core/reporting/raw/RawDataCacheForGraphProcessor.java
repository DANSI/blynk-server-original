package cc.blynk.server.core.reporting.raw;

import cc.blynk.utils.structure.LimitedArrayDeque;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw data storage for graph LIVE stream.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.01.17.
 */
public class RawDataCacheForGraphProcessor {

    private static final int GRAPH_CACHE_SIZE = 60;

    private final Map<BaseReportingKey, LimitedArrayDeque<Double>> rawStorage;

    public RawDataCacheForGraphProcessor() {
        rawStorage = new ConcurrentHashMap<>();
    }

    public void collect(BaseReportingKey baseReportingKey, double doubleValue) {
        LimitedArrayDeque<Double> cache = rawStorage.get(baseReportingKey);
        if (cache == null) {
            cache = new LimitedArrayDeque<>(GRAPH_CACHE_SIZE);
            rawStorage.put(baseReportingKey, cache);
        }
        cache.add(doubleValue);
    }

}

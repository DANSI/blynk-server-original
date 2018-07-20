package cc.blynk.server.core.reporting.raw;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.reporting.GraphPinRequest;
import cc.blynk.utils.structure.LimitedArrayDeque;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.utils.FileUtils.SIZE_OF_REPORT_ENTRY;

/**
 * Raw data storage for graph LIVE stream.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.01.17.
 */
public class RawDataCacheForGraphProcessor {

    private static final int GRAPH_CACHE_SIZE = 60;

    public final ConcurrentHashMap<BaseReportingKey, LimitedArrayDeque<GraphValue>> rawStorage;

    public RawDataCacheForGraphProcessor() {
        rawStorage = new ConcurrentHashMap<>();
    }

    public void collect(BaseReportingKey baseReportingKey, GraphValue graphCacheValue) {
        LimitedArrayDeque<GraphValue> cache = rawStorage.get(baseReportingKey);
        if (cache == null) {
            cache = new LimitedArrayDeque<>(GRAPH_CACHE_SIZE);
            rawStorage.put(baseReportingKey, cache);
        }
        cache.add(graphCacheValue);
    }

    public ByteBuffer getLiveGraphData(User user, GraphPinRequest graphPinRequest) {
        LimitedArrayDeque<GraphValue> cache = rawStorage.get(new BaseReportingKey(user, graphPinRequest));

        if (cache != null && cache.size() > graphPinRequest.skipCount) {
            return toByteBuffer(cache, graphPinRequest.count, graphPinRequest.skipCount);
        }

        return null;
    }

    private ByteBuffer toByteBuffer(LimitedArrayDeque<GraphValue> cache, int count, int skipCount) {
        int size = cache.size();
        int expectedMinimumLength = count + skipCount;
        int diff = size - expectedMinimumLength;
        int startReadIndex = Math.max(0, diff);
        int expectedResultSize = diff < 0 ? count + diff : count;
        if (expectedResultSize <= 0) {
            return null;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(expectedResultSize * SIZE_OF_REPORT_ENTRY);

        int i = 0;
        int counter = 0;
        for (GraphValue graphValue : cache) {
            if (startReadIndex <= i && counter < expectedResultSize) {
                counter++;
                byteBuffer.putDouble(graphValue.value)
                        .putLong(graphValue.ts);
            }
            i++;
        }
        return byteBuffer;
    }
}

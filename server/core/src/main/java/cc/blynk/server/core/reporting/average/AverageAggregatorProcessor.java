package cc.blynk.server.core.reporting.average;

import cc.blynk.server.core.reporting.raw.BaseReportingKey;
import cc.blynk.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.internal.SerializationUtil.deserialize;
import static cc.blynk.server.internal.SerializationUtil.serialize;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class AverageAggregatorProcessor implements Closeable {

    private static final Logger log = LogManager.getLogger(AverageAggregatorProcessor.class);

    public static final long MINUTE = 1000 * 60;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;
    static final String MINUTE_TEMP_FILENAME = "minute_temp.bin";
    static final String HOURLY_TEMP_FILENAME = "hourly_temp.bin";
    static final String DAILY_TEMP_FILENAME = "daily_temp.bin";
    private final String dataFolder;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> minute;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> hourly;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> daily;

    @SuppressWarnings("unchecked")
    public AverageAggregatorProcessor(String dataFolder) {
        this.dataFolder = dataFolder;

        Path path;

        path = Paths.get(dataFolder, MINUTE_TEMP_FILENAME);
        this.minute = (ConcurrentHashMap<AggregationKey, AggregationValue>) deserialize(path);
        FileUtils.deleteQuietly(path);

        path = Paths.get(dataFolder, HOURLY_TEMP_FILENAME);
        this.hourly = (ConcurrentHashMap<AggregationKey, AggregationValue>) deserialize(path);
        FileUtils.deleteQuietly(path);

        path = Paths.get(dataFolder, DAILY_TEMP_FILENAME);
        this.daily = (ConcurrentHashMap<AggregationKey, AggregationValue>) deserialize(path);
        FileUtils.deleteQuietly(path);
    }

    private static void aggregate(Map<AggregationKey, AggregationValue> map, AggregationKey key, double value) {
        AggregationValue aggregationValue = map.get(key);
        if (aggregationValue == null) {
            aggregationValue = map.putIfAbsent(key, new AggregationValue(value));
            if (aggregationValue == null) {
                return;
            }
        }

        aggregationValue.update(value);
    }

    public void collect(BaseReportingKey baseReportingKey, long ts, double val) {
        aggregate(minute, new AggregationKey(baseReportingKey, ts / MINUTE), val);
        aggregate(hourly, new AggregationKey(baseReportingKey, ts / HOUR), val);
        aggregate(daily, new AggregationKey(baseReportingKey, ts / DAY), val);
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getMinute() {
        return minute;
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getHourly() {
        return hourly;
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getDaily() {
        return daily;
    }

    @Override
    public void close() {
        if (minute.size() > 100_000) {
            log.info("Too many minute records ({}). "
                    + "This may cause performance issues on server start. Skipping.", minute.size());
        } else {
            serialize(Paths.get(dataFolder, MINUTE_TEMP_FILENAME), minute);
        }
        serialize(Paths.get(dataFolder, HOURLY_TEMP_FILENAME), hourly);
        serialize(Paths.get(dataFolder, DAILY_TEMP_FILENAME), daily);
    }

}

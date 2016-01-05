package cc.blynk.server.core.reporting.average;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.utils.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.utils.ReportingUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class AverageAggregator {

    public static final long MINUTE = 1000 * 60;
    public static final long HOUR = 1000 * 60 * 60;
    public static final long DAY = 24 * HOUR;
    public static final String MINUTE_TEMP_FILENAME = "minute_temp.bin";
    public static final String HOURLY_TEMP_FILENAME = "hourly_temp.bin";
    public static final String DAILY_TEMP_FILENAME = "daily_temp.bin";
    public final String dataFolder;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> minute;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> hourly;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> daily;

    public AverageAggregator(String dataFolder) {
        this.dataFolder = dataFolder;

        Path path;

        //todo move this logic to separate class?
        path = Paths.get(dataFolder, MINUTE_TEMP_FILENAME);
        this.minute = read(path);
        FileUtils.deleteQuietly(path);

        path = Paths.get(dataFolder, HOURLY_TEMP_FILENAME);
        this.hourly = read(path);
        FileUtils.deleteQuietly(path);

        path = Paths.get(dataFolder, DAILY_TEMP_FILENAME);
        this.daily = read(path);
        FileUtils.deleteQuietly(path);
    }

    private static void aggregate(Map<AggregationKey, AggregationValue> map, AggregationKey key, double value) {
        AggregationValue aggregationValue = map.get(key);
        if (aggregationValue == null) {
            final AggregationValue aggregationValueTmp = new AggregationValue();
            aggregationValue = map.putIfAbsent(key, aggregationValueTmp);
            if (aggregationValue == null) {
                aggregationValue = aggregationValueTmp;
            }
        }

        aggregationValue.update(value);
    }

    public void collect(String username, int dashId, PinType pinType, byte pin, long ts, String value) {
        try {
            double val = Double.parseDouble(value);
            aggregate(minute, new AggregationKey(username, dashId, pinType, pin, ts / MINUTE), val);
            aggregate(hourly, new AggregationKey(username, dashId, pinType, pin, ts / HOUR), val);
            aggregate(daily, new AggregationKey(username, dashId, pinType, pin, ts / DAY), val);
        } catch (NumberFormatException e) {
            //value not a number so ignore. no way to make average aggregation
        }
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

    public void close() {
        write(Paths.get(dataFolder, MINUTE_TEMP_FILENAME), minute);
        write(Paths.get(dataFolder, HOURLY_TEMP_FILENAME), hourly);
        write(Paths.get(dataFolder, DAILY_TEMP_FILENAME), daily);
    }

}

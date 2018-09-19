package cc.blynk.server.core.reporting.average;

import cc.blynk.server.core.reporting.raw.BaseReportingKey;
import cc.blynk.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    public static final String MINUTE_TEMP_FILENAME = "minute_temp.bin";
    public static final String HOURLY_TEMP_FILENAME = "hourly_temp.bin";
    public static final String DAILY_TEMP_FILENAME = "daily_temp.bin";
    private final String dataFolder;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> minute;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> hourly;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> daily;

    public AverageAggregatorProcessor(String dataFolder) {
        this.dataFolder = dataFolder;

        Path path;

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
            write(Paths.get(dataFolder, MINUTE_TEMP_FILENAME), minute);
        }
        write(Paths.get(dataFolder, HOURLY_TEMP_FILENAME), hourly);
        write(Paths.get(dataFolder, DAILY_TEMP_FILENAME), daily);
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<AggregationKey, AggregationValue> read(Path path) {
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path);
                 ObjectInputStream objectinputstream = new ObjectInputStream(is)) {
                return (ConcurrentHashMap<AggregationKey, AggregationValue>) objectinputstream.readObject();
            } catch (Exception e) {
                log.error(e);
            }
        }

        return new ConcurrentHashMap<>();
    }

    private static void write(Path path, Map<AggregationKey, AggregationValue> map) {
        if (map.size() > 0) {
            try (OutputStream os = Files.newOutputStream(path);
                 ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeObject(map);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

}

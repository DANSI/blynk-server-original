package cc.blynk.server.storage.reporting.average;

import cc.blynk.server.model.enums.PinType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class AverageAggregator {

    public static final long HOURS = 1000 * 60 * 60;
    public static final long DAY = 24 * HOURS;
    private final static Logger log = LogManager.getLogger(AverageAggregator.class);
    private final ConcurrentHashMap<AggregationKey, AggregationValue> hourly;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> daily;

    private final String dataFolder;

    public AverageAggregator(String dataFolder) {
        this.dataFolder = dataFolder;
        this.hourly = read(dataFolder, "hourly_temp.bin");
        this.daily = read(dataFolder, "daily_temp.bin");
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

    private static ConcurrentHashMap<AggregationKey, AggregationValue> read(String dataFolder, String file) {
        Path path = Paths.get(dataFolder, file);
        try (InputStream is = Files.newInputStream(Paths.get(dataFolder, file))) {
            ObjectInputStream objectinputstream = new ObjectInputStream(is);
            ConcurrentHashMap<AggregationKey, AggregationValue> map = (ConcurrentHashMap<AggregationKey, AggregationValue>) objectinputstream.readObject();
            Files.deleteIfExists(path);
            return map;
        } catch (Exception e) {
            log.error(e);
        }
        return new ConcurrentHashMap<>();
    }

    private static void write(String dataFolder, String file, Map<AggregationKey, AggregationValue> map) {
        if (map.size() > 0) {
            try (OutputStream os = Files.newOutputStream(Paths.get(dataFolder, file))) {
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(map);
            } catch (Exception e) {
                log.error(e);
            }
        }
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

    public void close() {
        write(dataFolder, "hourly_temp.bin", hourly);
        write(dataFolder, "daily_temp.bin", daily);
    }

}

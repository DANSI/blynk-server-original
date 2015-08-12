package cc.blynk.server.workers;

import cc.blynk.server.storage.average.AggregationKey;
import cc.blynk.server.storage.average.AggregationValue;
import cc.blynk.server.storage.average.AverageAggregator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.storage.StorageDao.generateFilename;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class StorageWorker implements Runnable {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Logger log = LogManager.getLogger(StorageWorker.class);
    private static final Comparator<AggregationKey> AGGREGATION_KEY_COMPARATOR = (o1, o2) -> (int) (o1.ts - o2.ts);
    private final AverageAggregator averageAggregator;
    private final Path dataFolder;

    public StorageWorker(AverageAggregator averageAggregator, String dataFolder) {
        this.averageAggregator = averageAggregator;
        //data is hardcoded - move to properties. don't forget about log4j.
        this.dataFolder = Paths.get(dataFolder, "data");
    }

    @Override
    public void run() {
        long nowTruncatedToHours = System.currentTimeMillis() / AverageAggregator.HOURS;

        ConcurrentHashMap<AggregationKey, AggregationValue> map = averageAggregator.getMap();

        List<AggregationKey> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys, AGGREGATION_KEY_COMPARATOR);


        for (AggregationKey key : keys) {
            //if prev hour
            if (key.ts < nowTruncatedToHours) {
                AggregationValue value = map.get(key);
                Path userPath = Paths.get(dataFolder.toString(), key.username);
                if (Files.notExists(userPath)) {
                    try {
                        Files.createDirectories(userPath);
                    } catch (IOException e) {
                        log.error("Error creating user data reporting folder.", e);
                        continue;
                    }
                }

                double average = value.calcAverage();
                long eventTS = key.ts * AverageAggregator.HOURS;

                String fileName = generateFilename(key.dashId, key.pinType, key.pin);

                Path reportingPath = Paths.get(userPath.toString(), fileName);
                try (BufferedWriter writer = Files.newBufferedWriter(reportingPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    writer.write(average + "," + eventTS + LINE_SEPARATOR);
                } catch (IOException e) {
                    log.error("Error open user data reporting file.", e);
                }
                map.remove(key);
            }
        }
    }

}

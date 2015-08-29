package cc.blynk.server.storage;

import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.storage.reporting.average.AverageAggregator;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.07.15.
 */
public class StorageTest {

    @Test
    @Ignore
    public void generateLogFile() throws IOException {
        Path path = Paths.get("/home/doom369/test-data.log");
        GraphKey key = new GraphKey(100000, (byte) 10, PinType.ANALOG);
        long ts  = System.currentTimeMillis();
        int i = 0;
        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            //200 req/sec current load.
            for (int j = 0; j < 24 * 3600; j++) {
                StoreMessage storeMessage = new StoreMessage(key, String.valueOf(i++), ts++);
                bw.write(storeMessage.toCSV());
                bw.write("\n");
            }
        }
    }

    @Test
    @Ignore
    public void generateDailyHistoryData() throws IOException {
        Path path = Paths.get("/home/doom369/blynk/data/dmitriy@blynk.cc/daily_data.csv");
        //now - 365 days.
        long ts  = (System.currentTimeMillis() / AverageAggregator.DAY - 365);
        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            for (int i = 0; i < 365; i++ ) {
                bw.write(i + "," + (ts + i) * AverageAggregator.DAY);
                bw.newLine();
            }
        }
    }

    @Test
    @Ignore
    public void generateHourlyHistoryData() throws IOException {
        int count = 7 * 24;
        Path path = Paths.get("/home/doom369/blynk/data/dmitriy@blynk.cc/hourly_data.csv");
        //now - 1 week.
        long ts  = (System.currentTimeMillis() / AverageAggregator.HOURS - count);
        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            for (int i = 0; i < count; i++ ) {
                bw.write(i + "," + (ts + i) * AverageAggregator.HOURS);
                bw.newLine();
            }
        }
    }


}

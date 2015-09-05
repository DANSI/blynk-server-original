package cc.blynk.server.workers;

import cc.blynk.server.model.enums.GraphType;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.storage.reporting.average.AggregationKey;
import cc.blynk.server.storage.reporting.average.AggregationValue;
import cc.blynk.server.storage.reporting.average.AverageAggregator;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.storage.StorageDao.generateFilename;
import static cc.blynk.server.utils.ReportingUtil.getReportingFolder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.08.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageWorkerTest {

    private final String dataFolder = getReportingFolder(System.getProperty("java.io.tmpdir"));

    @Mock
    public AverageAggregator averageAggregator;

    @Before
    public void cleanup() {
        Path dataFolder1 = Paths.get(dataFolder, "test");
        try {
            FileUtils.deleteDirectory(dataFolder1.toFile());
        } catch (IOException e) {
        }

        Path dataFolder2 = Paths.get(dataFolder, "test2");
        try {
            FileUtils.deleteDirectory(dataFolder2.toFile());
        } catch (IOException e){
        }
    }

    @Test
    public void testStore() throws IOException {
        StorageWorker storageWorker = new StorageWorker(averageAggregator, dataFolder);

        ConcurrentHashMap<AggregationKey, AggregationValue> map = new ConcurrentHashMap<>();

        long ts = getTS() / AverageAggregator.HOURS;

        AggregationKey aggregationKey = new AggregationKey("test", 1, PinType.ANALOG, (byte) 1, ts);
        AggregationValue aggregationValue = new AggregationValue();
        aggregationValue.update(100);
        AggregationKey aggregationKey2 = new AggregationKey("test", 1, PinType.ANALOG, (byte) 1, ts - 1);
        AggregationValue aggregationValue2 = new AggregationValue();
        aggregationValue2.update(150.54);
        AggregationKey aggregationKey3 = new AggregationKey("test2", 2, PinType.ANALOG, (byte) 2, ts);
        AggregationValue aggregationValue3 = new AggregationValue();
        aggregationValue3.update(200);

        map.put(aggregationKey, aggregationValue);
        map.put(aggregationKey2, aggregationValue2);
        map.put(aggregationKey3, aggregationValue3);

        when(averageAggregator.getHourly()).thenReturn(map);
        when(averageAggregator.getDaily()).thenReturn(new ConcurrentHashMap<>());

        createFolders("test");
        createFolders("test2");

        storageWorker.run();

        assertTrue(Files.exists(Paths.get(dataFolder, "test", generateFilename(1, PinType.ANALOG, (byte) 1, GraphType.HOURLY))));
        assertTrue(Files.exists(Paths.get(dataFolder, "test2", generateFilename(2, PinType.ANALOG, (byte) 2, GraphType.HOURLY))));

        byte[] data = StorageDao.getAllFromDisk(dataFolder, "test", 1, PinType.ANALOG, (byte) 1, 2, GraphType.HOURLY);
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        assertNotNull(data);
        assertEquals(32, data.length);

        assertEquals(150.54, byteBuffer.getDouble(), 0.001);
        assertEquals((ts - 1) * AverageAggregator.HOURS, byteBuffer.getLong());

        assertEquals(100.0, byteBuffer.getDouble(), 0.001);
        assertEquals(ts * AverageAggregator.HOURS, byteBuffer.getLong());

        data = StorageDao.getAllFromDisk(dataFolder, "test2", 2, PinType.ANALOG, (byte) 2, 1, GraphType.HOURLY);
        byteBuffer = ByteBuffer.wrap(data);
        assertNotNull(data);
        assertEquals(16, data.length);
        assertEquals(200.0, byteBuffer.getDouble(), 0.001);
        assertEquals(ts * AverageAggregator.HOURS, byteBuffer.getLong());
    }

    private void createFolders(String username) {
        Path userPath = Paths.get(dataFolder, username);
        if (Files.notExists(userPath)) {
            try {
                Files.createDirectories(userPath);
            } catch (IOException e) {
                System.out.println("Error creating user data reporting folder.");
                System.out.println(e);
            }
        }
    }

    private long getTS() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
        String dateInString = "Aug 10, 2015 12:10:56";

        try {

            Date date = formatter.parse(dateInString);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0;
    }

}

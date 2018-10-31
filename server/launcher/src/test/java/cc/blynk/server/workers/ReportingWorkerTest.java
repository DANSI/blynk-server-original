package cc.blynk.server.workers;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import cc.blynk.server.core.reporting.average.AverageAggregatorProcessor;
import cc.blynk.server.db.ReportingDBManager;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.properties.ServerProperties;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.core.dao.ReportingDiskDao.generateFilename;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.08.15.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReportingWorkerTest {

    private final static Logger log = LogManager.getLogger(ReportingWorkerTest.class);

    private final String reportingFolder = Paths.get(System.getProperty("java.io.tmpdir"), "data").toString();

    @Mock
    public AverageAggregatorProcessor averageAggregator;

    public ReportingDiskDao reportingDaoMock;

    @Mock
    public ServerProperties properties;

    private BlockingIOProcessor blockingIOProcessor;

    @Before
    public void cleanup() throws IOException {
        blockingIOProcessor = new BlockingIOProcessor(4, 1);
        Path dataFolder1 = Paths.get(reportingFolder, "test");
        FileUtils.deleteDirectory(dataFolder1.toFile());
        createReportingFolder(reportingFolder, "test");

        Path dataFolder2 = Paths.get(reportingFolder, "test2");
        FileUtils.deleteDirectory(dataFolder2.toFile());
        createReportingFolder(reportingFolder, "test2");

        reportingDaoMock = new ReportingDiskDao(reportingFolder, averageAggregator, true);
    }

    private static void createReportingFolder(String reportingFolder, String email) {
        Path reportingPath = Paths.get(reportingFolder, email);
        if (Files.notExists(reportingPath)) {
            try {
                Files.createDirectories(reportingPath);
            } catch (IOException ioe) {
                log.error("Error creating report folder. {}", reportingPath);
            }
        }
    }

    @Test
    public void testFailure() {
        User user = new User();
        user.email = "test";
        user.appName = AppNameUtil.BLYNK;
        ReportingWorker reportingWorker = new ReportingWorker(reportingDaoMock,
                reportingFolder, new ReportingDBManager(blockingIOProcessor, true));

        ConcurrentHashMap<AggregationKey, AggregationValue> map = new ConcurrentHashMap<>();

        long ts = getTS() / AverageAggregatorProcessor.HOUR;

        AggregationKey aggregationKey = new AggregationKey("ddd\0+123@gmail.com",
                AppNameUtil.BLYNK, 1, 0, PinType.ANALOG, (short) 1, ts);
        AggregationValue aggregationValue = new AggregationValue();
        aggregationValue.update(100);

        map.put(aggregationKey, aggregationValue);

        when(averageAggregator.getMinute()).thenReturn(map);

        reportingWorker.run();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testStore() {
        User user = new User();
        user.email = "test";
        user.appName = AppNameUtil.BLYNK;
        ReportingWorker reportingWorker = new ReportingWorker(reportingDaoMock,
                reportingFolder, new ReportingDBManager(blockingIOProcessor, true));

        ConcurrentHashMap<AggregationKey, AggregationValue> map = new ConcurrentHashMap<>();

        long ts = getTS() / AverageAggregatorProcessor.HOUR;

        AggregationKey aggregationKey = new AggregationKey("test", AppNameUtil.BLYNK, 1, 0, PinType.ANALOG, (short) 1, ts);
        AggregationValue aggregationValue = new AggregationValue();
        aggregationValue.update(100);
        AggregationKey aggregationKey2 = new AggregationKey("test", AppNameUtil.BLYNK, 1, 0, PinType.ANALOG, (short) 1, ts - 1);
        AggregationValue aggregationValue2 = new AggregationValue();
        aggregationValue2.update(150.54);
        AggregationKey aggregationKey3 = new AggregationKey("test2", AppNameUtil.BLYNK, 2, 0, PinType.ANALOG, (short) 2, ts);
        AggregationValue aggregationValue3 = new AggregationValue();
        aggregationValue3.update(200);

        map.put(aggregationKey, aggregationValue);
        map.put(aggregationKey2, aggregationValue2);
        map.put(aggregationKey3, aggregationValue3);

        when(averageAggregator.getMinute()).thenReturn(new ConcurrentHashMap<>());
        when(averageAggregator.getHourly()).thenReturn(map);
        when(averageAggregator.getDaily()).thenReturn(new ConcurrentHashMap<>());

        reportingWorker.run();

        assertTrue(Files.exists(Paths.get(reportingFolder, "test",
                generateFilename(1, 0, PinType.ANALOG, (short) 1, GraphGranularityType.HOURLY))));
        assertTrue(Files.exists(Paths.get(reportingFolder, "test2",
                generateFilename(2, 0, PinType.ANALOG, (short) 2, GraphGranularityType.HOURLY))));

        assertTrue(map.isEmpty());

        ByteBuffer data = reportingDaoMock.getByteBufferFromDisk(user, 1, 0, PinType.ANALOG, (short) 1, 2, GraphGranularityType.HOURLY, 0);
        assertNotNull(data);
        assertEquals(32, data.capacity());

        assertEquals(150.54, data.getDouble(), 0.001);
        assertEquals((ts - 1) * AverageAggregatorProcessor.HOUR, data.getLong());

        assertEquals(100.0, data.getDouble(), 0.001);
        assertEquals(ts * AverageAggregatorProcessor.HOUR, data.getLong());

        User user2 = new User();
        user2.email = "test2";
        user2.appName = AppNameUtil.BLYNK;
        data = reportingDaoMock.getByteBufferFromDisk(user2, 2, 0, PinType.ANALOG, (short) 2, 1, GraphGranularityType.HOURLY, 0);
        assertNotNull(data);
        assertEquals(16, data.capacity());
        assertEquals(200.0, data.getDouble(), 0.001);
        assertEquals(ts * AverageAggregatorProcessor.HOUR, data.getLong());
    }

    @Test
    public void testStore2() {
        ReportingWorker reportingWorker = new ReportingWorker(reportingDaoMock,
                reportingFolder, new ReportingDBManager(blockingIOProcessor, true));

        ConcurrentHashMap<AggregationKey, AggregationValue> map = new ConcurrentHashMap<>();

        long ts = getTS() / AverageAggregatorProcessor.HOUR;

        AggregationKey aggregationKey = new AggregationKey("test", AppNameUtil.BLYNK, 1, 0, PinType.ANALOG, (short) 1, ts);
        AggregationValue aggregationValue = new AggregationValue();
        aggregationValue.update(100);
        AggregationKey aggregationKey2 = new AggregationKey("test", AppNameUtil.BLYNK, 1, 0, PinType.ANALOG, (short) 1, ts - 1);
        AggregationValue aggregationValue2 = new AggregationValue();
        aggregationValue2.update(150.54);
        AggregationKey aggregationKey3 = new AggregationKey("test", AppNameUtil.BLYNK, 1, 0, PinType.ANALOG, (short) 1, ts - 2);
        AggregationValue aggregationValue3 = new AggregationValue();
        aggregationValue3.update(200);

        map.put(aggregationKey, aggregationValue);
        map.put(aggregationKey2, aggregationValue2);
        map.put(aggregationKey3, aggregationValue3);

        when(averageAggregator.getMinute()).thenReturn(new ConcurrentHashMap<>());
        when(averageAggregator.getHourly()).thenReturn(map);
        when(averageAggregator.getDaily()).thenReturn(new ConcurrentHashMap<>());

        reportingWorker.run();

        assertTrue(Files.exists(Paths.get(reportingFolder, "test",
                generateFilename(1, 0, PinType.ANALOG, (short) 1, GraphGranularityType.HOURLY))));

        assertTrue(map.isEmpty());

        User user = new User();
        user.email = "test";
        user.appName = AppNameUtil.BLYNK;

        //take less
        ByteBuffer data = reportingDaoMock.getByteBufferFromDisk(user, 1, 0, PinType.ANALOG, (short) 1, 1, GraphGranularityType.HOURLY, 0);
        assertNotNull(data);
        assertEquals(16, data.capacity());

        assertEquals(100.0, data.getDouble(), 0.001);
        assertEquals(ts * AverageAggregatorProcessor.HOUR, data.getLong());


        //take more
        data = reportingDaoMock.getByteBufferFromDisk(user, 1, 0, PinType.ANALOG, (short) 1, 24, GraphGranularityType.HOURLY, 0);
        assertNotNull(data);
        assertEquals(48, data.capacity());

        assertEquals(200.0, data.getDouble(), 0.001);
        assertEquals((ts - 2) * AverageAggregatorProcessor.HOUR, data.getLong());

        assertEquals(150.54, data.getDouble(), 0.001);
        assertEquals((ts - 1) * AverageAggregatorProcessor.HOUR, data.getLong());

        assertEquals(100.0, data.getDouble(), 0.001);
        assertEquals(ts * AverageAggregatorProcessor.HOUR, data.getLong());
    }


    @Test
    public void testDeleteCommand() {
        ReportingWorker reportingWorker = new ReportingWorker(reportingDaoMock,
                reportingFolder, new ReportingDBManager(blockingIOProcessor, true));

        ConcurrentHashMap<AggregationKey, AggregationValue> map = new ConcurrentHashMap<>();

        long ts = getTS() / AverageAggregatorProcessor.HOUR;

        AggregationKey aggregationKey = new AggregationKey("test", AppNameUtil.BLYNK, 1, 0, PinType.ANALOG, (short) 1, ts);
        AggregationValue aggregationValue = new AggregationValue();
        aggregationValue.update(100);
        AggregationKey aggregationKey2 = new AggregationKey("test", AppNameUtil.BLYNK, 1, 0, PinType.ANALOG, (short) 1, ts - 1);
        AggregationValue aggregationValue2 = new AggregationValue();
        aggregationValue2.update(150.54);
        AggregationKey aggregationKey3 = new AggregationKey("test2", AppNameUtil.BLYNK, 2, 0, PinType.ANALOG, (short) 2, ts);
        AggregationValue aggregationValue3 = new AggregationValue();
        aggregationValue3.update(200);

        map.put(aggregationKey, aggregationValue);
        map.put(aggregationKey2, aggregationValue2);
        map.put(aggregationKey3, aggregationValue3);

        when(averageAggregator.getMinute()).thenReturn(new ConcurrentHashMap<>());
        when(averageAggregator.getHourly()).thenReturn(map);
        when(averageAggregator.getDaily()).thenReturn(new ConcurrentHashMap<>());
        when(properties.getProperty("data.folder")).thenReturn(System.getProperty("java.io.tmpdir"));

        reportingWorker.run();

        assertTrue(Files.exists(Paths.get(reportingFolder, "test", generateFilename(1, 0, PinType.ANALOG, (short) 1, GraphGranularityType.HOURLY))));
        assertTrue(Files.exists(Paths.get(reportingFolder, "test2", generateFilename(2, 0, PinType.ANALOG, (short) 2, GraphGranularityType.HOURLY))));

        User user = new User();
        user.email = "test";
        user.appName = AppNameUtil.BLYNK;

        new ReportingDiskDao(reportingFolder, true).delete(user, 1, 0, PinType.ANALOG, (short) 1);
        assertFalse(Files.exists(Paths.get(reportingFolder, "test", generateFilename(1, 0, PinType.ANALOG, (short) 1, GraphGranularityType.HOURLY))));
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

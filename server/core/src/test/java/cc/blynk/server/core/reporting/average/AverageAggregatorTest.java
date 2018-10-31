package cc.blynk.server.core.reporting.average;

import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.raw.BaseReportingKey;
import cc.blynk.utils.AppNameUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static cc.blynk.server.core.reporting.average.AverageAggregatorProcessor.DAY;
import static cc.blynk.server.core.reporting.average.AverageAggregatorProcessor.HOUR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.08.15.
 */
public class AverageAggregatorTest {

    private final String reportingFolder = Paths.get(System.getProperty("java.io.tmpdir"), "data").toString();

    private static long getMillis(int year, int month, int dayOfMonth, int hour, int minute) {
        LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute);
        return dateTime.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
    }

    @Test
    public void testAverageWorksOkForOnePin() {
        AverageAggregatorProcessor averageAggregator = new AverageAggregatorProcessor("");
        User user = new User();
        user.email = "test@test.com";
        user.appName = AppNameUtil.BLYNK;

        PinType pinType = PinType.VIRTUAL;
        int dashId = 1;
        short pin = 1;

        long ts = getMillis(2015, 8, 1, 0, 0);

        int COUNT = 100;

        double expectedAverage = 0;
        for (int i = 0; i < COUNT; i++) {
            expectedAverage += i;
            averageAggregator.collect(new BaseReportingKey(user.email, user.appName, dashId, 0, pinType, pin), ts, i);
        }
        expectedAverage /= COUNT;

        assertEquals(1, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());

        assertEquals(expectedAverage, averageAggregator.getHourly().get(new AggregationKey(user.email, user.appName, dashId, 0, pinType, pin, ts / HOUR)).calcAverage(), 0);
        assertEquals(expectedAverage, averageAggregator.getDaily().get(new AggregationKey(user.email, user.appName, dashId, 0, pinType, pin, ts / DAY)).calcAverage(), 0);
    }

    @Test
    public void testAverageWorksForOneDay() {
        AverageAggregatorProcessor averageAggregator = new AverageAggregatorProcessor("");
        User user = new User();
        user.email = "test@test.com";
        user.appName = AppNameUtil.BLYNK;
        PinType pinType = PinType.VIRTUAL;
        int dashId = 1;
        short pin = 1;

        double expectedDailyAverage = 0;

        int COUNT = 100;

        for (int hour = 0; hour < 24; hour++) {
            long ts = getMillis(2015, 8, 1, hour, 0);

            double expectedAverage = 0;
            for (int i = 0; i < COUNT; i++) {
                expectedAverage += i;
                averageAggregator.collect(new BaseReportingKey(user.email, user.appName, dashId, 0, pinType, pin), ts, i);
            }
            expectedDailyAverage += expectedAverage;
            expectedAverage /= COUNT;

            assertEquals(hour + 1, averageAggregator.getHourly().size());

            assertEquals(expectedAverage, averageAggregator.getHourly().get(new AggregationKey(user.email, user.appName, dashId, 0, pinType, pin, ts / HOUR)).calcAverage(), 0);
        }
        expectedDailyAverage /= COUNT * 24;

        assertEquals(24, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());
        assertEquals(expectedDailyAverage, averageAggregator.getDaily().get(new AggregationKey(user.email, user.appName, dashId, 0, pinType, pin, getMillis(2015, 8, 1, 0, 0) / DAY)).calcAverage(), 0);
    }

    @Test
    public void testTempFilesCreated() throws IOException {
        Path dir = Paths.get(reportingFolder, "");
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }

        AverageAggregatorProcessor averageAggregator = new AverageAggregatorProcessor(reportingFolder);

        User user = new User();
        user.email = "test@test.com";
        user.appName = AppNameUtil.BLYNK;
        PinType pinType = PinType.VIRTUAL;
        int dashId = 1;
        short pin = 1;

        double expectedDailyAverage = 0;

        int COUNT = 100;

        for (int hour = 0; hour < 24; hour++) {
            long ts = getMillis(2015, 8, 1, hour, 0);

            double expectedAverage = 0;
            for (int i = 0; i < COUNT; i++) {
                expectedAverage += i;
                averageAggregator.collect(new BaseReportingKey(user.email, user.appName, dashId, 0, pinType, pin), ts, i);
            }
            expectedDailyAverage += expectedAverage;
            expectedAverage /= COUNT;

            assertEquals(hour + 1, averageAggregator.getHourly().size());

            assertEquals(expectedAverage, averageAggregator.getHourly().get(new AggregationKey(user.email, user.appName, dashId, 0, pinType, pin, ts / HOUR)).calcAverage(), 0);
        }
        expectedDailyAverage /= COUNT * 24;

        assertEquals(24, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());
        assertEquals(expectedDailyAverage, averageAggregator.getDaily().get(new AggregationKey(new BaseReportingKey(user.email, user.appName, dashId, 0, pinType, pin), getMillis(2015, 8, 1, 0, 0) / DAY)).calcAverage(), 0);


        averageAggregator.close();

        assertTrue(Files.exists(Paths.get(reportingFolder, AverageAggregatorProcessor.HOURLY_TEMP_FILENAME)));
        assertTrue(Files.exists(Paths.get(reportingFolder, AverageAggregatorProcessor.DAILY_TEMP_FILENAME)));

        averageAggregator = new AverageAggregatorProcessor(reportingFolder);

        assertEquals(24, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());
        assertEquals(expectedDailyAverage, averageAggregator.getDaily().get(new AggregationKey(new BaseReportingKey(user.email, user.appName, dashId, 0, pinType, pin), getMillis(2015, 8, 1, 0, 0) / DAY)).calcAverage(), 0);

        assertTrue(Files.notExists(Paths.get(reportingFolder, AverageAggregatorProcessor.HOURLY_TEMP_FILENAME)));
        assertTrue(Files.notExists(Paths.get(reportingFolder, AverageAggregatorProcessor.DAILY_TEMP_FILENAME)));

        ReportingDiskDao reportingDao = new ReportingDiskDao(reportingFolder, true);

        reportingDao.delete(user, dashId, 0, PinType.VIRTUAL, pin);
        assertTrue(Files.notExists(Paths.get(reportingFolder, AverageAggregatorProcessor.HOURLY_TEMP_FILENAME)));
        assertTrue(Files.notExists(Paths.get(reportingFolder, AverageAggregatorProcessor.DAILY_TEMP_FILENAME)));
    }

}

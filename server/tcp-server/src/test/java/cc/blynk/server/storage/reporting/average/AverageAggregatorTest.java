package cc.blynk.server.storage.reporting.average;

import cc.blynk.server.model.enums.PinType;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static cc.blynk.server.storage.reporting.average.AverageAggregator.DAY;
import static cc.blynk.server.storage.reporting.average.AverageAggregator.HOURS;
import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.08.15.
 */
public class AverageAggregatorTest {

    private static long getMillis(int year, int month, int dayOfMonth, int hour, int minute) {
        LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute);
        return dateTime.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
    }

    @Test
    public void testAverageWorksOkForOnePin() {
        AverageAggregator averageAggregator = new AverageAggregator("");
        String username = "test";
        PinType pinType = PinType.VIRTUAL;
        int dashId = 1;
        byte pin = 1;

        long ts = getMillis(2015, 8, 1, 0, 0);

        int COUNT = 100;

        double expectedAverage = 0;
        for (int i = 0; i < COUNT; i++) {
            expectedAverage += i;
            averageAggregator.collect(username, dashId, pinType, pin, ts, String.valueOf(i));
        }
        expectedAverage /= COUNT;

        assertEquals(1, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());

        assertEquals(expectedAverage, averageAggregator.getHourly().get(new AggregationKey(username, dashId, pinType, pin, ts / HOURS)).calcAverage(), 0);
        assertEquals(expectedAverage, averageAggregator.getDaily().get(new AggregationKey(username, dashId, pinType, pin, ts / DAY)).calcAverage(), 0);
    }

    @Test
    public void testAverageWorksForOneDay() {
        AverageAggregator averageAggregator = new AverageAggregator("");
        String username = "test";
        PinType pinType = PinType.VIRTUAL;
        int dashId = 1;
        byte pin = 1;

        double expectedDailyAverage = 0;

        int COUNT = 100;

        for (int hour = 0; hour < 24; hour++) {
            long ts = getMillis(2015, 8, 1, hour, 0);

            double expectedAverage = 0;
            for (int i = 0; i < COUNT; i++) {
                expectedAverage += i;
                averageAggregator.collect(username, dashId, pinType, pin, ts, String.valueOf(i));
            }
            expectedDailyAverage += expectedAverage;
            expectedAverage /= COUNT;

            assertEquals(hour + 1, averageAggregator.getHourly().size());

            assertEquals(expectedAverage, averageAggregator.getHourly().get(new AggregationKey(username, dashId, pinType, pin, ts / HOURS)).calcAverage(), 0);
        }
        expectedDailyAverage /= COUNT * 24;

        assertEquals(24, averageAggregator.getHourly().size());
        assertEquals(1, averageAggregator.getDaily().size());
        assertEquals(expectedDailyAverage, averageAggregator.getDaily().get(new AggregationKey(username, dashId, pinType, pin, getMillis(2015, 8, 1, 0, 0) / DAY)).calcAverage(), 0);
    }

}

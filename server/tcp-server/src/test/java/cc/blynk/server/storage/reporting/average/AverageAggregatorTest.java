package cc.blynk.server.storage.reporting.average;

import cc.blynk.server.model.enums.PinType;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static cc.blynk.server.storage.reporting.average.AverageAggregator.DAY;
import static cc.blynk.server.storage.reporting.average.AverageAggregator.HOURS;
import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.08.15.
 */
public class AverageAggregatorTest {

    private static ZoneId zoneId = ZoneId.of("America/Los_Angeles");

    @Test
    public void testAverageWorksOkForOnePin() {
        LocalDateTime dateTime = LocalDateTime.of(2015, 8, 1, 1, 0);
        ZonedDateTime zdt = dateTime.atZone(zoneId);
        long ts = zdt.toInstant().toEpochMilli();


        AverageAggregator averageAggregator = new AverageAggregator();
        String username = "test";
        PinType pinType = PinType.VIRTUAL;
        int dashId = 1;
        byte pin = 1;

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

}

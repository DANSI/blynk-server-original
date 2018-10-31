package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.16.
 */
public class ReportingDaoTest {

    final String REPORTING_MINUTE_FILE_NAME = "history_%s-0_%c%d_minute.bin";
    final String REPORTING_HOURLY_FILE_NAME = "history_%s-0_%c%d_hourly.bin";
    final String REPORTING_DAILY_FILE_NAME = "history_%s-0_%c%d_daily.bin";

    @Test
    public void testFileName() {
        int dashId = 1;
        PinType pinType = PinType.VIRTUAL;
        short pin = 2;

        assertEquals(String.format(REPORTING_MINUTE_FILE_NAME, dashId, pinType.pintTypeChar, pin),
                ReportingDiskDao.generateFilename(dashId, 0, pinType, pin, GraphGranularityType.MINUTE));

        assertEquals(String.format(REPORTING_HOURLY_FILE_NAME, dashId, pinType.pintTypeChar, pin),
                ReportingDiskDao.generateFilename(dashId, 0, pinType, pin, GraphGranularityType.HOURLY));

        assertEquals(String.format(REPORTING_DAILY_FILE_NAME, dashId, pinType.pintTypeChar, pin),
                ReportingDiskDao.generateFilename(dashId, 0, pinType, pin, GraphGranularityType.DAILY));

    }

}

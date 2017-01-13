package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.reporting.average.AverageAggregator;
import cc.blynk.server.core.stats.model.Stat;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class RealtimeStatsDBTest {

    private static DBManager dbManager;
    private static BlockingIOProcessor blockingIOProcessor;

    @BeforeClass
    public static void init() throws Exception {
        blockingIOProcessor = new BlockingIOProcessor(2, 10000, null);
        dbManager = new DBManager("db-test.properties", blockingIOProcessor);
        assertNotNull(dbManager.getConnection());
    }

    @AfterClass
    public static void close() {
        dbManager.close();
    }

    @Before
    public void cleanAll() throws Exception {
        //clean everything just in case
        dbManager.executeSQL("DELETE FROM users");
        dbManager.executeSQL("DELETE FROM reporting_average_minute");
        dbManager.executeSQL("DELETE FROM reporting_average_hourly");
        dbManager.executeSQL("DELETE FROM reporting_average_daily");
        dbManager.executeSQL("DELETE FROM purchase");
        dbManager.executeSQL("DELETE FROM redeem");
        dbManager.executeSQL("DELETE FROM reporting_app_stat_minute");
    }

    @Test
    public void testInsert1000RecordsAndSelect() throws Exception {
        String region = "ua";
        long now = System.currentTimeMillis();
        Stat stat = new Stat(1,2,3,4,5,6,7,8,9,10,now);

        dbManager.insertStat(region, stat);


        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from reporting_app_stat_minute")) {

            int i = 0;
            while (rs.next()) {
                assertEquals(region, rs.getString("region"));
                assertEquals((now / AverageAggregator.MINUTE) * AverageAggregator.MINUTE, rs.getLong("ts"));

                assertEquals(1, rs.getInt("minute_rate"));
                assertEquals(2, rs.getInt("registrations"));
                assertEquals(3, rs.getInt("active"));
                assertEquals(4, rs.getInt("active_week"));
                assertEquals(5, rs.getInt("active_month"));
                assertEquals(6, rs.getInt("connected"));
                assertEquals(7, rs.getInt("online_apps"));
                assertEquals(8, rs.getInt("total_online_apps"));
                assertEquals(9, rs.getInt("online_hards"));
                assertEquals(10, rs.getInt("total_online_hards"));
            }
            connection.commit();
        }


    }


}

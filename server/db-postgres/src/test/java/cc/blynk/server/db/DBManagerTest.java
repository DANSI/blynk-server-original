package cc.blynk.server.db;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import cc.blynk.server.core.reporting.average.AverageAggregator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cc.blynk.server.db.DBManager.*;
import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
@Ignore("Requires real DB for test")
public class DBManagerTest {

    private static DBManager dbManager;

    @BeforeClass
    public static void init() throws Exception {
        dbManager = new DBManager("db-test.properties");
        assertNotNull(dbManager.getConnection());

        //copy paste from create_schema.sql
        dbManager.executeSQL("DELETE FROM users");
        dbManager.executeSQL("DELETE FROM reporting_average_minute");
        dbManager.executeSQL("DELETE FROM reporting_average_hourly");
        dbManager.executeSQL("DELETE FROM reporting_average_daily");
    }

    @AfterClass
    public static void close() {
        dbManager.close();
    }

    @Test
    public void test() throws Exception {
        assertNotNull(dbManager.getConnection());
    }

    @Test
    public void testInsert1000RecordsAndSelect() throws Exception {
        System.out.println("Starting");

        int a = 0;

        long startMinute = 0;
        long start = System.currentTimeMillis();
        try (Connection connection = dbManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(DBManager.insertMinute)) {

            String userName = "test{}@gmail.com";
            long minute = (System.currentTimeMillis() / AverageAggregator.MINUTE) * AverageAggregator.MINUTE;
            startMinute = minute;
            for (int i = 0; i < 1000; i++) {
                String newUserName = userName.replace("{}", "" + i);
                prepareReportingInsert(ps, newUserName, 1, (byte) 0, PinType.VIRTUAL, minute, (double) i);
                ps.addBatch();
                minute += AverageAggregator.MINUTE;
                a++;
            }

            ps.executeBatch();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finished : " + (System.currentTimeMillis() - start)  + " millis. Executed : " + a);


        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from reporting_average_minute order by ts ASC")) {

            int i = 0;
            String userName = "test{}@gmail.com";
            while (rs.next()) {
                assertEquals(userName.replace("{}", "" + i), rs.getString("username"));
                assertEquals(1, rs.getInt("project_id"));
                assertEquals(0, rs.getByte("pin"));
                assertEquals("v", rs.getString("pinType"));
                assertEquals(startMinute, rs.getLong("ts"));
                assertEquals((double) i, rs.getDouble("value"), 0.0001);
                startMinute += AverageAggregator.MINUTE;
                i++;
            }
        }
    }

    @Test
    public void testManyConnections() throws Exception {
        List<User> users = new ArrayList<>();
        User user = new User("test@test", "1");
        users.add(user);

        Map<AggregationKey, AggregationValue> map = new HashMap<>();
        AggregationValue value = new AggregationValue();
        value.update(1);
        for (int i = 0; i < 60; i++) {
            map.clear();
            map.put(new AggregationKey("test@test.com", i, PinType.ANALOG, (byte) 1, 0), value);
            dbManager.insertReporting(map, GraphType.MINUTE);
            dbManager.insertReporting(map, GraphType.HOURLY);
            dbManager.insertReporting(map, GraphType.DAILY);
            dbManager.saveUsers(users);
            Thread.sleep(1000);
        }

    }

    @Test
    public void testUpsertUser() throws Exception {
        List<User> users = new ArrayList<>();
        users.add(new User("test@gmail.com", "pass"));
        dbManager.saveUsers(users);

        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from users where username = 'test@gmail.com'")) {
            while (rs.next()) {
                assertEquals("test@gmail.com", rs.getString("username"));
            }
        }
    }

    @Test
    public void testSelect() {
        long ts = 1455924480000L;
        try (Connection connection = dbManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(DBManager.selectMinute)) {

             prepareReportingSelect(ps, ts, 2);
             ResultSet rs = ps.executeQuery();


            while(rs.next()) {
                System.out.println(rs.getLong("ts") + " " + rs.getDouble("value"));
            }

            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}

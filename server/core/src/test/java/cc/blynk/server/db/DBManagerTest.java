package cc.blynk.server.db;

import cc.blynk.server.core.BlockingIOProcessor;
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
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.zip.GZIPOutputStream;

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
        BlockingIOProcessor blockingIOProcessor = new BlockingIOProcessor(1, 10, null);
        dbManager = new DBManager("db-test.properties", blockingIOProcessor);
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
             PreparedStatement ps = connection.prepareStatement(ReportingDBDao.insertMinute)) {

            String userName = "test{}@gmail.com";
            long minute = (System.currentTimeMillis() / AverageAggregator.MINUTE) * AverageAggregator.MINUTE;
            startMinute = minute;
            for (int i = 0; i < 1000; i++) {
                String newUserName = userName.replace("{}", "" + i);
                ReportingDBDao.prepareReportingInsert(ps, newUserName, 1, (byte) 0, PinType.VIRTUAL, minute, (double) i);
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
    public void testCopy100RecordsIntoFile() throws Exception {
        System.out.println("Starting");

        int a = 0;

        long start = System.currentTimeMillis();
        try (Connection connection = dbManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(ReportingDBDao.insertMinute)) {

            String userName = "test@gmail.com";
            long minute = (System.currentTimeMillis() / AverageAggregator.MINUTE) * AverageAggregator.MINUTE;

            for (int i = 0; i < 100; i++) {
                ReportingDBDao.prepareReportingInsert(ps, userName, 1, (byte) 0, PinType.VIRTUAL, minute, (double) i);
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
             Writer gzipWriter = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File("/home/doom369/output.csv.gz"))), "UTF-8")) {

            CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));


            String selectQuery = "select pintype || pin, ts, value from reporting_average_minute where project_id = 1 and username = 'test@gmail.com'";
            long res = copyManager.copyOut("COPY (" + selectQuery + " ) TO STDOUT WITH (FORMAT CSV)", gzipWriter);
            System.out.println(res);
        }


    }

    @Test
    public void testDeleteWorksAsExpected() throws Exception {
        long minute = 0;
        try (Connection connection = dbManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(ReportingDBDao.insertMinute)) {

            minute = (System.currentTimeMillis() / AverageAggregator.MINUTE) * AverageAggregator.MINUTE;

            for (int i = 0; i < 370; i++) {
                ReportingDBDao.prepareReportingInsert(ps, "test1111@gmail.com", 1, (byte) 0, PinType.VIRTUAL, minute, (double) i);
                ps.addBatch();
                minute += AverageAggregator.MINUTE;
            }

            ps.executeBatch();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Instant now = Instant.ofEpochMilli(minute);
        dbManager.cleanOldReportingRecords(now);

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
        users.add(new User("test@gmail.com", "pass2"));
        users.add(new User("test2@gmail.com", "pass2"));
        dbManager.saveUsers(users);

        try (Connection connection = dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from users where username = 'test@gmail.com'")) {
            while (rs.next()) {
                assertEquals("test@gmail.com", rs.getString("username"));
                assertNull("region", rs.getString("region"));
            }
            connection.commit();
        }
    }

    @Test
    public void testRedeem() throws Exception {
        assertNull(dbManager.selectRedeemByToken("123"));
        String token = UUID.randomUUID().toString().replace("-", "");
        dbManager.executeSQL("insert into redeem (token) values('" + token + "')");
        assertNotNull(dbManager.selectRedeemByToken(token));
        assertNull(dbManager.selectRedeemByToken("123"));
    }

    @Test
    public void testOptimisticLockingRedeem() throws Exception {
        String token = UUID.randomUUID().toString().replace("-", "");
        dbManager.executeSQL("insert into redeem (token) values('" + token + "')");

        Redeem redeem = dbManager.selectRedeemByToken(token);
        assertNotNull(redeem);
        assertEquals(redeem.token, token);
        assertFalse(redeem.isRedeemed);
        assertEquals(1, redeem.version);

        assertTrue(dbManager.updateRedeem("user@user.com", token));
        assertFalse(dbManager.updateRedeem("user@user.com", token));

        redeem = dbManager.selectRedeemByToken(token);
        assertNotNull(redeem);
        assertEquals(redeem.token, token);
        assertTrue(redeem.isRedeemed);
        assertEquals(2, redeem.version);
        assertEquals("user@user.com", redeem.username);
    }

    @Test
    public void testSelect() {
        long ts = 1455924480000L;
        try (Connection connection = dbManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(ReportingDBDao.selectMinute)) {

            ReportingDBDao.prepareReportingSelect(ps, ts, 2);
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

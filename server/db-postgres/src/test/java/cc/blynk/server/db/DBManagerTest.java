package cc.blynk.server.db;

import cc.blynk.server.core.reporting.average.AverageAggregator;
import cc.blynk.utils.ServerProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static cc.blynk.server.db.DBManager.*;
import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.02.16.
 */
public class DBManagerTest {

    private static DBManager dbManager;

    @BeforeClass
    public static void init() throws Exception{
        dbManager = new DBManager(new ServerProperties("db.properties"));
        assertNotNull(dbManager.getConnection());
    }

    @AfterClass
    public static void close() {
        dbManager.close();
    }

    @Test
    public void test() {

    }


    @Test
    public void testInsertMinute() throws Exception {
        System.out.println("Starting");

        int a = 0;

        for (int count = 0; count < 1000; count++) {
            long start = System.currentTimeMillis();
            try (Connection connection = dbManager.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(DBManager.insertMinute)) {

                String userName = "test{}@gmail.com";
                long minute = (System.currentTimeMillis() / AverageAggregator.MINUTE) * AverageAggregator.MINUTE;
                for (int i = count * 1000; i < (count + 1) * 1000; i++) {
                    String newUserName = userName.replace("{}", String.valueOf(i));
                    prepareReportingInsert(preparedStatement, newUserName, 1, (byte) 0, 'v', minute, (double) i);
                    preparedStatement.addBatch();
                    minute += AverageAggregator.MINUTE;
                    a++;
                }

                preparedStatement.executeBatch();
                connection.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Finished : " + (System.currentTimeMillis() - start)  + " millis. Executed : " + a);
        }

    }


}

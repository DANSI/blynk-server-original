package cc.blynk.server.db;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.average.AverageAggregator;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.QueryTrace;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.02.16.
 */
public class CassandraTestBase {

    static SimpleClient client;

    @BeforeClass
    public static void init() {
        client = new SimpleClient("reporting", "46.101.225.223");
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @Test
    public void connect() {

    }

    @Test
    public void simpleLoadTest() throws Exception {
        System.out.println("Starting");
        long start = System.currentTimeMillis();
        int a = 0;
        for (int count = 0; count < 10; count++) {
            String userName = "test{}@gmail.com";
            long minute = (System.currentTimeMillis() / AverageAggregator.MINUTE) * AverageAggregator.MINUTE;
            for (int i = 0; i < 100; i++) {
                String newUserName = userName.replace("{}", String.valueOf(i));
                client.session.execute(client.reporting.insertIntoReportingMinute.bind(newUserName, 1, 0, "v", minute, (double) i));
                minute += AverageAggregator.MINUTE;
                a++;
            }
        }

        System.out.println("Finished : " + (System.currentTimeMillis() - start) / 1000  + " seconds. Executed : " + a);
    }

    private void logTraceInfo(ExecutionInfo executionInfo) {
        for (QueryTrace.Event event : executionInfo.getQueryTrace().getEvents()) {
            System.out.println(event);
        }

        System.out.println("Coordinator used " + executionInfo.getQueryTrace().getCoordinator());
        System.out.println("Duration in microseconds " + executionInfo.getQueryTrace().getDurationMicros());
    }

    @Test
    public void selectAll() {
        ResultSet results = client.session.execute(client.reporting.selectFromReportingMinute.bind("test0@gmail.com", 1, 0, "v"));

        //Iterable i = results.iterator();

        for (Row row : results) {
            System.out.println("ts : " + row.getLong("ts") + "; value : " + row.getDouble("value"));
        }
    }


    @Test
    public void testInsertSomeData() {

            client.reporting.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis(), 0.1);
            client.reporting.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 1, 0.2);
            client.reporting.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 2, 0.3);
            client.reporting.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 3, 0.4);
            client.reporting.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() - 1000, 0.0);
            client.reporting.insertIntoReportingMinute.bind("test@gmail.com", 1, 1, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis(), 1.1);

    }

    @Test
    public void selectSomeData() {
        ResultSet results = client.session.execute(client.reporting.selectFromReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), 10));

        //Iterable i = results.iterator();

        for (Row row : results) {
            System.out.println("ts : " + row.getLong("ts") + "; value : " + row.getDouble("value"));
        }

    }

}

package cc.blynk.server.db;

import cc.blynk.server.core.model.enums.PinType;
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
        client = new SimpleClient("mykeyspace", "127.0.0.1");
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @Test
    public void testInsertSomeData() {
        client.batch(
            client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis(), 0.1),
            client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 1, 0.2),
            client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 2, 0.3),
            client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 3, 0.4),
            client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() - 1000, 0.0),
            client.insertIntoReportingMinute.bind("test@gmail.com", 1, 1, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis(), 1.1)
        );
    }

    @Test
    public void selectSomeData() {
        ResultSet results = client.session.execute(client.selectFromReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), 10));

        //Iterable i = results.iterator();

        for (Row row : results) {
            System.out.println("ts : " + row.getLong("ts") + "; value : " + row.getDouble("value"));
        }

    }

}

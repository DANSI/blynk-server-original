package cc.blynk.server.db;

import cc.blynk.server.core.model.enums.PinType;
import com.datastax.driver.core.BatchStatement;
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
        client = new SimpleClient("127.0.0.1");
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @Test
    public void testInsertSomeData() {
        BatchStatement batch = new BatchStatement();
        batch.add(client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis(), 0.1));
        batch.add(client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 1, 0.2));
        batch.add(client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 2, 0.3));
        batch.add(client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() + 3, 0.4));
        batch.add(client.insertIntoReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis() - 1000, 0.0));
        batch.add(client.insertIntoReportingMinute.bind("test@gmail.com", 1, 1, Character.toString(PinType.VIRTUAL.pintTypeChar), System.currentTimeMillis(), 1.1));
        client.session.execute(batch);
    }

    @Test
    public void selectSomeData() {
        ResultSet results = client.session.execute(client.selectFromReportingMinute.bind("test@gmail.com", 1, 0, Character.toString(PinType.VIRTUAL.pintTypeChar)));

        //Iterable i = results.iterator();

        for (Row row : results) {
            System.out.println("ts : " + row.getLong("ts") + "; value : " + row.getDouble("value"));
        }

    }

}

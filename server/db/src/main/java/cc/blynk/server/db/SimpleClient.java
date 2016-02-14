package cc.blynk.server.db;

import com.datastax.driver.core.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.02.16.
 */
public class SimpleClient {

    public final Session session;
    public final PreparedStatement insertIntoReportingMinute;
    public final PreparedStatement selectFromReportingMinute;
    private final Cluster cluster;

    public SimpleClient(String nodeIp) {
        cluster = Cluster.builder()
                .addContactPoint(nodeIp)
                .build();

        Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
        for (Host host : metadata.getAllHosts() ) {
            System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack());
        }

        session = cluster.connect();

        insertIntoReportingMinute = session.prepare("INSERT INTO mykeyspace.report_average_minute " +
                "(username, project_id, pin, pinType, ts, value) " +
                "VALUES(?, ?, ?, ?, ?, ?) USING TTL 21600");

        selectFromReportingMinute = session.prepare("SELECT ts, value FROM mykeyspace.report_average_minute where " +
                "username = ? AND project_id = ? AND pin = ? AND pinType = ? LIMIT 3");
    }


    public static void main(String[] args) {
        SimpleClient client = new SimpleClient("127.0.0.1");
        client.close();
    }

    public void close() {
        session.close();
        cluster.close();
    }
}
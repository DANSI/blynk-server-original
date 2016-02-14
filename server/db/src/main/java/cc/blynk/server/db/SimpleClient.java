package cc.blynk.server.db;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.02.16.
 */
public class SimpleClient {

    public final Session session;
    private final Cluster cluster;
    public PreparedStatement insertIntoReportingMinute;
    public PreparedStatement selectFromReportingMinute;

    public SimpleClient(String keyspace, String nodeIp) {
        cluster = Cluster.builder()
                .addContactPoint(nodeIp)
                .build();

        Metadata metadata = cluster.getMetadata();
        System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
        for (Host host : metadata.getAllHosts() ) {
            System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(), host.getAddress(), host.getRack());
        }

        session = cluster.connect();

        initStatements(keyspace);
    }

    public static void main(String[] args) {
        SimpleClient client = new SimpleClient("mykeyspace", "127.0.0.1");
        client.close();
    }

    private void initStatements(String keyspace) {
        Insert insertHourly = QueryBuilder.insertInto(keyspace, "report_average_minute")
                .value("username", bindMarker())
                .value("project_id",  bindMarker())
                .value("pin",  bindMarker())
                .value("pinType",  bindMarker())
                .value("ts",  bindMarker())
                .value("value", bindMarker());
        insertHourly.using(ttl(21600));

        System.out.println("Preparing : " + insertHourly.toString());
        insertIntoReportingMinute = session.prepare(insertHourly);

        Select selectHourlyGraph = QueryBuilder.select("ts", "value")
                .from(keyspace, "report_average_minute");
        selectHourlyGraph.where(eq("username", bindMarker()));
        selectHourlyGraph.where(eq("project_id", bindMarker()));
        selectHourlyGraph.where(eq("pin", bindMarker()));
        selectHourlyGraph.where(eq("pinType", bindMarker()));
        selectHourlyGraph.limit(bindMarker());

        System.out.println("Preparing : " + selectHourlyGraph.toString());
        selectFromReportingMinute = session.prepare(selectHourlyGraph);
    }

    public void batch(BoundStatement... statements) {
        BatchStatement batch = new BatchStatement();
        for (BoundStatement statement : statements) {
            batch.add(statement);
        }
        session.execute(batch);
    }

    public void close() {
        session.close();
        cluster.close();
    }
}
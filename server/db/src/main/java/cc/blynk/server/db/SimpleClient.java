package cc.blynk.server.db;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.02.16.
 */
public class SimpleClient {

    public final Session session;
    public final ReportingQueries reporting;

    private final Cluster cluster;

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

        reporting = new ReportingQueries(keyspace, session);
    }

    public void close() {
        session.close();
        cluster.close();
    }
}
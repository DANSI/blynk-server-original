package cc.blynk.server.dao.graph;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.15.
 */
public class QueueMessage {

    public GraphKey key;

    public String body;

    public QueueMessage(GraphKey key, String body) {
        this.key = key;
        this.body = body;
    }
}

package cc.blynk.server.model.graph;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.15.
 */
public class StoreMessage {

    public final GraphKey key;

    public final String value;

    public final long ts;

    public StoreMessage(GraphKey key, String value, long ts) {
        this.key = key;
        this.value = value;
        this.ts = ts;
    }

    @Override
    public String toString() {
        return value + " " + ts;
    }

    public String toCSV() {
        return value + "," + ts;
    }
}

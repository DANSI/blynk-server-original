package cc.blynk.server.storage;

import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.model.Profile;

import java.util.Queue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public interface Storage {

    String store(Profile profile, Integer dashId, String body, int msgId);

    Queue<String> getAll(GraphKey key);

}

package cc.blynk.server.storage;

import cc.blynk.server.dao.graph.GraphInMemoryStorage;
import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.model.Profile;

import java.util.Queue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class StorageDao {

    private GraphInMemoryStorage graphInMemoryStorage;

    public StorageDao(int inMemoryStorageLimit) {
        this.graphInMemoryStorage = new GraphInMemoryStorage(inMemoryStorageLimit);
    }

    public StoreMessage process(Profile profile, Integer dashId, String body, int msgId) {
        return graphInMemoryStorage.store(profile, dashId, body, msgId);
    }

    public Queue<StoreMessage> getAllFromMemmory(GraphKey key) {
        return graphInMemoryStorage.getAll(key);
    }

}

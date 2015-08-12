package cc.blynk.server.storage;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.graph.GraphInMemoryStorage;
import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.storage.average.AverageAggregator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.Queue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class StorageDao {

    private static final Logger log = LogManager.getLogger(StorageDao.class);

    private final GraphInMemoryStorage graphInMemoryStorage;
    private final AverageAggregator averageAggregator;

    private volatile boolean ENABLE_RAW_DATA_STORE;

    public StorageDao(int inMemoryStorageLimit, AverageAggregator averageAggregator) {
        this.graphInMemoryStorage = new GraphInMemoryStorage(inMemoryStorageLimit);
        this.averageAggregator = averageAggregator;
    }

    public StoreMessage process(Profile profile, int dashId, String body) {
        PinType pinType = PinType.getPingType(body.charAt(0));

        String[] bodyParts = body.split(StringUtils.BODY_SEPARATOR_STRING);

        byte pin = Byte.parseByte(bodyParts[1]);

        GraphKey key = new GraphKey(dashId, pin, pinType);
        StoreMessage storeMessage = new StoreMessage(key, bodyParts[2], System.currentTimeMillis());

        if (ENABLE_RAW_DATA_STORE) {
            ThreadContext.put("dashId", Integer.toString(dashId));
            ThreadContext.put("pin", String.valueOf(body.charAt(0)) + pin);
            log.info(storeMessage.toCSV());
        }

        averageAggregator.collect(ThreadContext.get("user"), dashId, pinType, pin, storeMessage.ts, storeMessage.value);

        if (profile.hasGraphPin(key)) {
            graphInMemoryStorage.store(storeMessage);
            return storeMessage;
        }

        return null;
    }

    public Queue<StoreMessage> getAllFromMemmory(GraphKey key) {
        return graphInMemoryStorage.getAll(key);
    }

    public void updateProperties(ServerProperties props) {
        try {
            this.ENABLE_RAW_DATA_STORE = props.getBoolProperty("enable.raw.data.store");
        } catch (RuntimeException e) {
            //error already logged, so do nothing.
        }
    }

}

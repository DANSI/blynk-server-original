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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class StorageDao {

    public static final String REPORTING_FILE_NAME = "history_%s_%s.csv";
    private static final Logger log = LogManager.getLogger(StorageDao.class);
    private final GraphInMemoryStorage graphInMemoryStorage;
    private final AverageAggregator averageAggregator;
    private final String dataFolder;

    private volatile boolean ENABLE_RAW_DATA_STORE;

    public StorageDao(int inMemoryStorageLimit, AverageAggregator averageAggregator, String dataFolder) {
        this.graphInMemoryStorage = new GraphInMemoryStorage(inMemoryStorageLimit);
        this.averageAggregator = averageAggregator;
        this.dataFolder = Paths.get(dataFolder, "data").toString();
    }

    public static String generateFilename(int dashId, PinType pinType, byte pin) {
        return String.format(REPORTING_FILE_NAME, dashId, String.valueOf(pinType.pintTypeChar) + pin);
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

    public Collection<StoreMessage> getAllFromMemmory(int dashId, PinType pinType, byte pin) {
        return graphInMemoryStorage.getAll(new GraphKey(dashId, pin, pinType));
    }

    public Collection<?> getAllFromDisk(String username, int dashId, PinType pinType, byte pin, int periodInHours) {
        Path userDataFile = Paths.get(dataFolder, username, generateFilename(dashId, pinType, pin));
        if (Files.notExists(userDataFile)) {
            return Collections.emptyList();
        }

        //todo increase buffer size?
        try (BufferedReader reader = Files.newBufferedReader(userDataFile, StandardCharsets.UTF_8)) {
            List<String> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }

            if (result.size() > periodInHours) {
                return result.subList(result.size() - periodInHours, result.size());
            }

            return result;
        } catch (IOException e) {
            log.error(e);
        }

        return Collections.emptyList();
    }

    public void updateProperties(ServerProperties props) {
        try {
            this.ENABLE_RAW_DATA_STORE = props.getBoolProperty("enable.raw.data.store");
        } catch (RuntimeException e) {
            //error already logged, so do nothing.
        }
    }
}

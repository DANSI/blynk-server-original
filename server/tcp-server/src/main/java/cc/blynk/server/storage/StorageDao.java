package cc.blynk.server.storage;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.graph.GraphInMemoryStorage;
import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.enums.GraphType;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.storage.reporting.average.AverageAggregator;
import cc.blynk.server.utils.ReportingUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

import static cc.blynk.server.utils.ReportingUtil.EMPTY_ARRAY;
import static java.lang.String.format;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class StorageDao {

    public static final String REPORTING_HOURLY_FILE_NAME = "history_%s_%c%d_hourly.bin";
    public static final String REPORTING_DAILY_FILE_NAME = "history_%s_%c%d_daily.bin";
    private static final Logger log = LogManager.getLogger(StorageDao.class);
    private final GraphInMemoryStorage graphInMemoryStorage;
    private final AverageAggregator averageAggregator;
    private final String dataFolder;

    private volatile boolean ENABLE_RAW_DATA_STORE;

    public StorageDao(int inMemoryStorageLimit, AverageAggregator averageAggregator, String dataFolder) {
        this.graphInMemoryStorage = new GraphInMemoryStorage(inMemoryStorageLimit);
        this.averageAggregator = averageAggregator;
        this.dataFolder = ReportingUtil.getReportingFolder(dataFolder);
    }

    public static String generateFilename(int dashId, PinType pinType, byte pin, GraphType type) {
        if (type == GraphType.HOURLY) {
            return format(REPORTING_HOURLY_FILE_NAME, dashId, pinType.pintTypeChar, pin);
        }
        return format(REPORTING_DAILY_FILE_NAME, dashId, pinType.pintTypeChar, pin);
    }

    public static byte[] getAllFromDisk(String dataFolder, String username, int dashId, PinType pinType, byte pin, int count, GraphType type) {
        Path userDataFile = Paths.get(dataFolder, username, generateFilename(dashId, pinType, pin, type));
        if (Files.notExists(userDataFile)) {
            return EMPTY_ARRAY;
        }

        try (SeekableByteChannel channel = Files.newByteChannel(userDataFile, StandardOpenOption.READ)) {
            final int size = (int) Files.size(userDataFile);
            final int dataSize = count * 16;
            final int readDataSize = Math.min(dataSize, size);

            ByteBuffer buf = ByteBuffer.allocate(readDataSize);
            channel.position(Math.max(0, size - dataSize));
            channel.read(buf);

            //todo filter outdated records?

            return buf.array();
        } catch (IOException e) {
            log.error(e);
        }

        return EMPTY_ARRAY;
    }

    public void delete(String username, int dashId, PinType pinType, byte pin) {
        Path userDataHourlyFile = Paths.get(dataFolder, username, format(REPORTING_HOURLY_FILE_NAME, dashId, pinType.pintTypeChar, pin));
        Path userDataDailyFile = Paths.get(dataFolder, username, format(REPORTING_DAILY_FILE_NAME, dashId, pinType.pintTypeChar, pin));
        FileUtils.deleteQuietly(userDataHourlyFile.toFile());
        FileUtils.deleteQuietly(userDataDailyFile.toFile());
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

    public byte[] getAllFromDisk(String username, int dashId, PinType pinType, byte pin, int count, GraphType type) {
        return getAllFromDisk(dataFolder, username, dashId, pinType, pin, count, type);
    }

    public void updateProperties(ServerProperties props) {
        try {
            this.ENABLE_RAW_DATA_STORE = props.getBoolProperty("enable.raw.data.store");
        } catch (RuntimeException e) {
            //error already logged, so do nothing.
        }
    }
}

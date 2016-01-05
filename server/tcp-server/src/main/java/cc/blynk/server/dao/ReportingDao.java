package cc.blynk.server.dao;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.exceptions.NoDataException;
import cc.blynk.server.model.enums.GraphType;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.model.graph.GraphKey;
import cc.blynk.server.reporting.GraphPinRequest;
import cc.blynk.server.reporting.average.AverageAggregator;
import cc.blynk.server.utils.FileUtils;
import cc.blynk.server.utils.ReportingUtil;
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

import static cc.blynk.server.utils.ReportingUtil.*;
import static java.lang.String.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class ReportingDao {

    public static final String REPORTING_MINUTE_FILE_NAME = "history_%s_%c%d_minute.bin";
    public static final String REPORTING_HOURLY_FILE_NAME = "history_%s_%c%d_hourly.bin";
    public static final String REPORTING_DAILY_FILE_NAME = "history_%s_%c%d_daily.bin";
    private static final Logger log = LogManager.getLogger(ReportingDao.class);
    private final AverageAggregator averageAggregator;
    private final String dataFolder;

    private final boolean ENABLE_RAW_DATA_STORE;

    public ReportingDao(AverageAggregator averageAggregator, ServerProperties serverProperties) {
        this.averageAggregator = averageAggregator;
        this.dataFolder = ReportingUtil.getReportingFolder(serverProperties.getProperty("data.folder"));
        this.ENABLE_RAW_DATA_STORE = serverProperties.getBoolProperty("enable.raw.data.store");
    }

    public static String generateFilename(int dashId, PinType pinType, byte pin, GraphType type) {
        switch (type) {
            case MINUTE :
                return format(REPORTING_MINUTE_FILE_NAME, dashId, pinType.pintTypeChar, pin);
            case HOURLY :
                return format(REPORTING_HOURLY_FILE_NAME, dashId, pinType.pintTypeChar, pin);
            default :
                return format(REPORTING_DAILY_FILE_NAME, dashId, pinType.pintTypeChar, pin);
        }
    }

    //todo move out from event loop
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

    private static boolean checkNoData(byte[][] data) {
        boolean noData = true;

        for (byte[] pinData : data) {
            noData = noData && pinData.length == 0;
        }

        return noData;
    }

    public void delete(String username, int dashId, PinType pinType, byte pin) {
        log.debug("Removing {}{} pin data for dashId {}.", pinType.pintTypeChar, pin, dashId);
        Path userDataMinuteFile = Paths.get(dataFolder, username, format(REPORTING_MINUTE_FILE_NAME, dashId, pinType.pintTypeChar, pin));
        Path userDataHourlyFile = Paths.get(dataFolder, username, format(REPORTING_HOURLY_FILE_NAME, dashId, pinType.pintTypeChar, pin));
        Path userDataDailyFile = Paths.get(dataFolder, username, format(REPORTING_DAILY_FILE_NAME, dashId, pinType.pintTypeChar, pin));
        FileUtils.deleteQuietly(userDataMinuteFile);
        FileUtils.deleteQuietly(userDataHourlyFile);
        FileUtils.deleteQuietly(userDataDailyFile);
    }

    public void process(String username, GraphKey key) {
        if (ENABLE_RAW_DATA_STORE) {
            ThreadContext.put("dashId", Integer.toString(key.dashId));
            ThreadContext.put("pin", String.valueOf(key.pinType.pintTypeChar) + key.pin);
            log.info(key.toCSV());
        }

        averageAggregator.collect(username, key.dashId, key.pinType, key.pin, key.ts, key.value);
    }

    public byte[][] getAllFromDisk(String username, GraphPinRequest[] requestedPins, int msgId) {
        byte[][] values = new byte[requestedPins.length][];

        for (int i = 0; i < requestedPins.length; i++) {
            values[i] = getAllFromDisk(username,
                    requestedPins[i].dashId, requestedPins[i].pinType,
                    requestedPins[i].pin, requestedPins[i].count, requestedPins[i].type);
        }


        if (checkNoData(values)) {
            throw new NoDataException(msgId);
        }

        return values;
    }

    private byte[] getAllFromDisk(String username, int dashId, PinType pinType, byte pin, int count, GraphType type) {
        return getAllFromDisk(dataFolder, username, dashId, pinType, pin, count, type);
    }

}

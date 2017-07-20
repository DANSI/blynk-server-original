package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.protocol.exceptions.NoDataException;
import cc.blynk.server.core.reporting.GraphPinRequest;
import cc.blynk.server.core.reporting.average.AverageAggregatorProcessor;
import cc.blynk.server.core.reporting.raw.BaseReportingKey;
import cc.blynk.server.core.reporting.raw.GraphValue;
import cc.blynk.server.core.reporting.raw.RawDataCacheForGraphProcessor;
import cc.blynk.server.core.reporting.raw.RawDataProcessor;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.NumberUtil;
import cc.blynk.utils.ServerProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cc.blynk.utils.ArrayUtil.EMPTY_BYTES;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class ReportingDao implements Closeable {

    private static final Logger log = LogManager.getLogger(ReportingDao.class);

    public final AverageAggregatorProcessor averageAggregator;
    public final RawDataCacheForGraphProcessor rawDataCacheForGraphProcessor;
    public final RawDataProcessor rawDataProcessor;
    public final CSVGenerator csvGenerator;

    public final String dataFolder;

    private final boolean ENABLE_RAW_DB_DATA_STORE;

    //for test only
    public ReportingDao(String reportingFolder, AverageAggregatorProcessor averageAggregator, ServerProperties serverProperties) {
        this.averageAggregator = averageAggregator;
        this.rawDataCacheForGraphProcessor = new RawDataCacheForGraphProcessor();
        this.dataFolder = reportingFolder;
        this.ENABLE_RAW_DB_DATA_STORE = serverProperties.getBoolProperty("enable.raw.db.data.store");
        this.rawDataProcessor = new RawDataProcessor(ENABLE_RAW_DB_DATA_STORE);
        this.csvGenerator = new CSVGenerator(this);
    }

    public ReportingDao(String reportingFolder , ServerProperties serverProperties) {
        this.averageAggregator = new AverageAggregatorProcessor(reportingFolder);
        this.rawDataCacheForGraphProcessor = new RawDataCacheForGraphProcessor();
        this.dataFolder = reportingFolder;
        this.ENABLE_RAW_DB_DATA_STORE = serverProperties.getBoolProperty("enable.raw.db.data.store");
        this.rawDataProcessor = new RawDataProcessor(ENABLE_RAW_DB_DATA_STORE);
        this.csvGenerator = new CSVGenerator(this);
    }

    public static String generateFilename(int dashId, int deviceId, char pinType, byte pin, GraphGranularityType type) {
        switch (type) {
            case MINUTE :
                return formatMinute(dashId, deviceId, pinType, pin);
            case HOURLY :
                return formatHour(dashId, deviceId, pinType, pin);
            default :
                return formatDaily(dashId, deviceId, pinType, pin);
        }
    }

    public static ByteBuffer getByteBufferFromDisk(String dataFolder, User user, int dashId, int deviceId,
                                                   PinType pinType, byte pin, int count, GraphGranularityType type, int skipCount) {
        Path userDataFile = Paths.get(
                dataFolder,
                FileUtils.getUserReportingDir(user),
                generateFilename(dashId, deviceId, pinType.pintTypeChar, pin, type)
        );
        if (Files.exists(userDataFile)) {
            try {
                return FileUtils.read(userDataFile, count, skipCount);
            } catch (IOException ioe) {
                log.error(ioe);
            }
        }

        return null;
    }

    private static boolean hasData(byte[][] data) {
        for (byte[] pinData : data) {
            if (pinData.length > 0) {
                return true;
            }
        }
        return false;
    }

    public ByteBuffer getByteBufferFromDisk(User user, GraphPinRequest graphPinRequest) {
        return getByteBufferFromDisk(dataFolder, user,
                graphPinRequest.dashId, graphPinRequest.deviceId,
                graphPinRequest.pinType, graphPinRequest.pin,
                graphPinRequest.count, graphPinRequest.type,
                graphPinRequest.skipCount
        );
    }

    public ByteBuffer getByteBufferFromDisk(User user, int dashId, int deviceId, PinType pinType, byte pin, int count, GraphGranularityType type) {
        return getByteBufferFromDisk(dataFolder, user, dashId, deviceId, pinType, pin, count, type, 0);
    }

    public void delete(User user, int dashId, int deviceId, PinType pinType, byte pin) {
        log.debug("Removing {}{} pin data for dashId {}, deviceId {}.", pinType.pintTypeChar, pin, dashId, deviceId);
        Path userDataMinuteFile = Paths.get(dataFolder, FileUtils.getUserReportingDir(user), formatMinute(dashId, deviceId, pinType.pintTypeChar, pin));
        Path userDataHourlyFile = Paths.get(dataFolder, FileUtils.getUserReportingDir(user), formatHour(dashId, deviceId, pinType.pintTypeChar, pin));
        Path userDataDailyFile = Paths.get(dataFolder, FileUtils.getUserReportingDir(user), formatDaily(dashId, deviceId, pinType.pintTypeChar, pin));
        FileUtils.deleteQuietly(userDataMinuteFile);
        FileUtils.deleteQuietly(userDataHourlyFile);
        FileUtils.deleteQuietly(userDataDailyFile);
    }

    protected static String formatMinute(int dashId, int deviceId, char pinType, byte pin) {
        return format("minute", dashId, deviceId, pinType, pin);
    }

    protected static String formatHour(int dashId, int deviceId, char pinType, byte pin) {
        return format("hourly", dashId, deviceId, pinType, pin);
    }

    protected static String formatDaily(int dashId, int deviceId, char pinType, byte pin) {
        return format("daily", dashId, deviceId, pinType, pin);
    }

    private static String format(String type, int dashId, int deviceId, char pinType, byte pin) {
        //todo this is back compatibility code. should be removed in future versions.
        if (deviceId == 0) {
            return "history_" + dashId + "_" + pinType + pin + "_" + type + ".bin";
        }
        return "history_" + dashId + DEVICE_SEPARATOR + deviceId + "_" + pinType + pin + "_" + type + ".bin";
    }

    public void process(User user, int dashId, int deviceId, byte pin, PinType pinType, String value, long ts) {
        try {
            double doubleVal = NumberUtil.parseDouble(value);
            process(user, dashId, deviceId, pin, pinType, value, ts, doubleVal);
        } catch (Exception e) {
            //just in case
            log.trace("Error collecting reporting entry.");
        }
    }

    private void process(User user, int dashId, int deviceId, byte pin, PinType pinType, String value, long ts, double doubleVal) {
        if (ENABLE_RAW_DB_DATA_STORE) {
            rawDataProcessor.collect(new BaseReportingKey(user.email, user.appName, dashId, deviceId, pinType, pin), ts, value, doubleVal);
        }

        //not a number, nothing to aggregate
        if (doubleVal == NumberUtil.NO_RESULT) {
            return;
        }

        BaseReportingKey key = new BaseReportingKey(user.email, user.appName, dashId, deviceId, pinType, pin);
        averageAggregator.collect(key, ts, doubleVal);
        rawDataCacheForGraphProcessor.collect(key, new GraphValue(doubleVal, ts));
    }

    public byte[][] getReportingData(User user, GraphPinRequest[] requestedPins) {
        byte[][] values = new byte[requestedPins.length][];

        for (int i = 0; i < requestedPins.length; i++) {
            GraphPinRequest graphPinRequest = requestedPins[i];
            //live graph data is not on disk but in memory
            ByteBuffer byteBuffer;
            if (graphPinRequest.isLiveData()) {
                byteBuffer = rawDataCacheForGraphProcessor.getLiveGraphData(user, graphPinRequest);
            } else {
                byteBuffer = getByteBufferFromDisk(user, graphPinRequest);
            }
            values[i] = byteBuffer == null ? EMPTY_BYTES : byteBuffer.array();
        }

        if (!hasData(values)) {
            throw new NoDataException();
        }

        return values;
    }

    @Override
    public void close() {
        System.out.println("Stopping aggregator...");
        this.averageAggregator.close();
    }
}

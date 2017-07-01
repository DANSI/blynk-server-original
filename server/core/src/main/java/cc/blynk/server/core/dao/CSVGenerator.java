package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.GraphGranularityType;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.exceptions.NoDataException;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

/**
 * Simply generates CSV file from reporting data.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 08.03.17.
 */
public class CSVGenerator {

    private static final Logger log = LogManager.getLogger(CSVGenerator.class);

    public static final String CSV_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "blynk").toString();

    static {
        try {
            Files.createDirectories(Paths.get(CSV_DIR));
        } catch (IOException ioe) {
            log.error("Error creating temp '{}' folder for csv export data.", CSV_DIR);
        }
    }

    private final ReportingDao reportingDao;
    private final static int FETCH_COUNT = 60 * 24 * 30;

    public CSVGenerator(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    public Path createCSV(User user, int dashId, int inDeviceId, PinType pinType, byte pin, int... deviceIds) throws Exception {
        if (pinType == null || pin == Pin.NO_PIN) {
            throw new IllegalCommandBodyException("Wrong pin format.");
        }

        Path path = generateExportCSVPath(user.email, dashId, inDeviceId, pinType, pin);

        try (OutputStream output = Files.newOutputStream(path);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(output), CharsetUtil.US_ASCII))) {

            int emptyDataCounter = 0;
            for (int deviceId : deviceIds) {
                ByteBuffer onePinData = reportingDao.getByteBufferFromDisk(user, dashId, deviceId, pinType, pin, FETCH_COUNT, GraphGranularityType.MINUTE);
                if (onePinData != null) {
                    onePinData.flip();
                    writeBuf(writer, onePinData, deviceId);
                } else {
                    emptyDataCounter++;
                }
            }
            if (emptyDataCounter == deviceIds.length) {
                throw new NoDataException();
            }
        }

        return path;
    }

    private static void writeBuf(BufferedWriter writer, ByteBuffer onePinData, int deviceId) throws Exception {;
        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            writer.write("" + value + ',' + ts + ',' + deviceId + '\n');
        }
    }

    private static Path generateExportCSVPath(String email, int dashId, int deviceId, PinType pinType, byte pin) {
        return Paths.get(CSV_DIR, format(email, dashId, deviceId, pinType, pin));
    }

    //"%s_%s_%c%d.csv.gz"
    private static String format(String email, int dashId, int deviceId, PinType pinType, byte pin) {
        return email + "_" + dashId + "_" + deviceId + "_" + pinType.pintTypeChar + pin + ".csv.gz";
    }
}

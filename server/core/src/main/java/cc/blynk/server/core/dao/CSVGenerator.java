package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.protocol.exceptions.NoDataException;
import io.netty.util.CharsetUtil;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

import static cc.blynk.utils.FileUtils.CSV_DIR;
import static cc.blynk.utils.FileUtils.writeBufToCsv;

/**
 * Simply generates CSV file from reporting data.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 08.03.17.
 */
public class CSVGenerator {

    //43200 == 30 * 24 * 60 is minutes points for 1 month
    //todo move to limits
    private final static int FETCH_COUNT = Integer.parseInt(System.getProperty("csv.export.data.points.max", "43200"));
    private final ReportingDiskDao reportingDao;

    CSVGenerator(ReportingDiskDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    public Path createCSV(User user, int dashId, int inDeviceId, PinType pinType, short pin, int... deviceIds)
            throws Exception {
        if (!DataStream.isValid(pin, pinType)) {
            throw new IllegalStateException("Wrong pin format.");
        }

        Path path = generateExportCSVPath(user.email, dashId, inDeviceId, pinType, pin);

        try (OutputStream output = Files.newOutputStream(path);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                     new GZIPOutputStream(output), CharsetUtil.US_ASCII))) {

            int emptyDataCounter = 0;
            for (int deviceId : deviceIds) {
                ByteBuffer onePinData = reportingDao.getByteBufferFromDisk(user, dashId, deviceId,
                        pinType, pin, FETCH_COUNT, GraphGranularityType.MINUTE, 0);
                if (onePinData != null) {
                    writeBufToCsv(writer, onePinData, deviceId);
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

    private static Path generateExportCSVPath(String email, int dashId, int deviceId, PinType pinType, short pin) {
        return Paths.get(CSV_DIR, format(email, dashId, deviceId, pinType, pin));
    }

    public static final String EXPORT_CSV_EXTENSION = ".csv.gz";

    //"%s_%s_%c%d.csv.gz"
    private static String format(String email, int dashId, int deviceId, PinType pinType, short pin) {
        long now = System.currentTimeMillis();
        return email + "_" + dashId + "_" + deviceId + "_"
                + pinType.pintTypeChar + pin + "_" + now + EXPORT_CSV_EXTENSION;
    }
}

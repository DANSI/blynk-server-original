package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.exceptions.NoDataException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
    private final int FETCH_COUNT;

    public CSVGenerator(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
        this.FETCH_COUNT = 60 * 24 * 30;
    }

    public Path createCSV(User user, int dashId, int deviceId, PinType pinType, byte pin) throws Exception {
        if (pinType == null || pin == Pin.NO_PIN) {
            throw new IllegalCommandBodyException("Wrong pin format.");
        }

        //data for 1 month
        ByteBuffer onePinData = reportingDao.getByteBufferFromDisk(user, dashId, deviceId, pinType, pin, FETCH_COUNT, GraphType.MINUTE);
        if (onePinData == null) {
            throw new NoDataException();
        }

        onePinData.flip();
        Path path = generateExportCSVPath(user.name, dashId, pinType, pin);
        makeGzippedCSVFile(onePinData, path);
        return path;
    }

    /**
     * Writes ByteBuffer with value (double 8 bytes),
     * timestamp (long 8 bytes) data to disk as csv file and gzips it.
     *
     * @param onePinData - reporting data
     * @param path - path to file to store data
     * @throws IOException
     */
    private static void makeGzippedCSVFile(ByteBuffer onePinData, Path path) throws IOException {
        try (OutputStream output = Files.newOutputStream(path);
             Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), "UTF-8")) {

            while (onePinData.remaining() > 0) {
                double value = onePinData.getDouble();
                long ts = onePinData.getLong();

                writer.write("" + value);
                writer.write(',');
                writer.write("" + ts);
                writer.write('\n');
            }
        }
    }

    private static Path generateExportCSVPath(String username, int dashId, PinType pinType, byte pin) {
        return Paths.get(CSV_DIR, format(username, dashId, pinType, pin));
    }

    //"%s_%s_%c%d.csv.gz"
    private static String format(String username, int dashId, PinType pinType, byte pin) {
        return username + "_" + dashId + "_" + pinType.pintTypeChar + pin + ".csv.gz";
    }
}

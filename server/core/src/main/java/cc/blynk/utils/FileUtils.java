package cc.blynk.utils;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.model.enums.PinType;
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

import static java.lang.String.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public class FileUtils {

    public static final String CSV_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "blynk").toString();

    private final static Logger log = LogManager.getLogger(FileUtils.class);

    static {
        try {
            Files.createDirectories(Paths.get(CSV_DIR));
        } catch (IOException ioe) {
            log.error("Error creating temp '{}' folder for csv export data.", CSV_DIR);
        }
    }

    public static boolean deleteQuietly(Path path) {
        try {
            return path.toFile().delete();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Writes ByteBuffer with value (double 8 bytes),
     * timestamp (long 8 bytes) data to disk as csv file and gzips it.
     *
     * @param onePinData - reporting data
     * @param path - path to file to store data
     * @throws IOException
     */
    public static void makeGzippedCSVFile(ByteBuffer onePinData, Path path) throws IOException {
        try (OutputStream output = Files.newOutputStream(path);
             Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), "UTF-8")) {

            while (onePinData.remaining() > 0) {
                double value = onePinData.getDouble();
                long ts = onePinData.getLong();

                writer.write(String.valueOf(value));
                writer.write(',');
                writer.write(String.valueOf(ts));
                writer.write('\n');
            }
        }
    }

    public static final int DEFAULT_FETCH_COUNT = 60 * 24 * 30 * 1;
    public static final String EXPORT_GRAPH_FILENAME = "%s_%s_%c%d.csv.gz";

    public static Path createCSV(ReportingDao reportingDao, String username, int dashId, PinType pinType, byte pin) {
        if (pinType != null && pin > -1) {
            //data for 1 month

            ByteBuffer onePinData = reportingDao.getByteBufferFromDisk(username, dashId, pinType, pin, DEFAULT_FETCH_COUNT, GraphType.MINUTE);
            if (onePinData != null) {
                onePinData.flip();
                String filename = format(EXPORT_GRAPH_FILENAME, username, dashId, pinType.pintTypeChar, pin);
                Path path = Paths.get(CSV_DIR, filename);
                try {
                    makeGzippedCSVFile(onePinData, path);
                    return path;
                } catch (IOException e) {
                    log.warn("Error making csv file for data export. Reason : {}", e.getMessage());
                }
            }
        }
        return null;
    }
}

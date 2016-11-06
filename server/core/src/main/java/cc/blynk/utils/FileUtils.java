package cc.blynk.utils;

import cc.blynk.server.core.dao.ReportingDao;
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
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPOutputStream;

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

    public static boolean move(Path source, Path target) {
        try {
            Path targetFile = Paths.get(target.toString(), source.getFileName().toString());
            Files.move(source, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.debug("Failed to move file. {}" , e.getMessage());
            return false;
        }
        return true;
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

    public static Path createCSV(ReportingDao reportingDao, String username, int dashId, PinType pinType, byte pin) throws Exception {
        if (pinType == null || pin == -1) {
            throw new IllegalCommandBodyException("Wrong pin format.");
        }

        //data for 1 month
        ByteBuffer onePinData = reportingDao.getByteBufferFromDisk(username, dashId, pinType, pin, DEFAULT_FETCH_COUNT, GraphType.MINUTE);
        if (onePinData == null) {
            throw new NoDataException();
        }

        onePinData.flip();
        Path path = generateExportCSVPath(username, dashId, pinType, pin);
        makeGzippedCSVFile(onePinData, path);
        return path;
    }

    private static Path generateExportCSVPath(String username, int dashId, PinType pinType, byte pin) {
        return Paths.get(CSV_DIR, format(username, dashId, pinType, pin));
    }

    //"%s_%s_%c%d.csv.gz"
    private static String format(String username, int dashId, PinType pinType, byte pin) {
        return username + "_" + dashId + "_" + pinType.pintTypeChar + pin + ".csv.gz";
    }
}

package cc.blynk.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public final class FileUtils {

    private final static Logger log = LogManager.getLogger(FileUtils.class);

    private static final String BLYNK_FOLDER = "blynk";
    public static final String CSV_DIR = Paths.get(
            System.getProperty("java.io.tmpdir"), BLYNK_FOLDER)
            .toString();

    private FileUtils() {
    }

    static {
        try {
            Files.createDirectories(Paths.get(CSV_DIR));
        } catch (IOException ioe) {
            log.error("Error creating temp '{}' folder for csv export data.", CSV_DIR);
        }
    }

    private static final String[] POSSIBLE_LOCAL_PATHS = new String[] {
            "./server/http-dashboard/target/classes",
            "./server/http-api/target/classes",
            "./server/http-admin/target/classes",
            "./server/http-core/target/classes",
            "./server/core/target",
            "../server/http-admin/target/classes",
            "../server/http-dashboard/target/classes",
            "../server/http-core/target/classes",
            "../server/core/target",
            "./server/utils/target",
            "../server/utils/target",
            Paths.get(System.getProperty("java.io.tmpdir"), BLYNK_FOLDER).toString()
    };

    //reporting entry is long value (8 bytes) + timestamp (8 bytes)
    public static final int SIZE_OF_REPORT_ENTRY = 16;

    public static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            log.trace("Error during '{}' folder removal.", path);
            //ignore
        }
    }

    public static boolean move(Path source, Path target) {
        try {
            Path targetFile = Paths.get(target.toString(), source.getFileName().toString());
            Files.move(source, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.debug("Failed to move file. {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Simply writes single reporting entry to disk (16 bytes).
     * Reporting entry is value (double) and timestamp (long)
     *
     * @param reportingPath - path to user specific reporting file
     * @param value         - sensor data
     * @param ts            - time when entry was created
     */
    public static void write(Path reportingPath, double value, long ts) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                Files.newOutputStream(reportingPath, CREATE, APPEND))) {
            dos.writeDouble(value);
            dos.writeLong(ts);
            dos.flush();
        }
    }

    /**
     * Read bunch of last records from file.
     *
     * @param userDataFile - file to read
     * @param count = number of records to read
     *
     * @return - byte buffer with data
     */
    public static ByteBuffer read(Path userDataFile, int count) throws IOException {
        return read(userDataFile, count, 0);
    }

    /**
     * Read bunch of last records from file.
     *
     * @param userDataFile - file to read
     * @param count        - number of records to read
     * @param skip         - number of entries to skip from the end
     * @return - byte buffer with data
     */
    public static ByteBuffer read(Path userDataFile, int count, int skip) throws IOException {
        int size = (int) Files.size(userDataFile);
        int expectedMinimumLength = (count + skip) * SIZE_OF_REPORT_ENTRY;
        int diff = size - expectedMinimumLength;
        int startReadIndex = Math.max(0, diff);
        int bufferSize = diff < 0 ? count * SIZE_OF_REPORT_ENTRY + diff : count * SIZE_OF_REPORT_ENTRY;
        if (bufferSize <= 0) {
            return null;
        }

        ByteBuffer buf = ByteBuffer.allocate(bufferSize);

        try (SeekableByteChannel channel = Files.newByteChannel(userDataFile, EnumSet.of(READ))) {
            channel.position(startReadIndex)
                    .read(buf);
            ((Buffer) buf).flip();
            return buf;
        }
    }

    public static void writeBufToCsvFilterAndFormat(ByteArrayOutputStream baos, ByteBuffer onePinData,
                                                    String pin, String deviceName,
                                                    long startFrom, DateTimeFormatter formatter) {

        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            if (startFrom <= ts) {
                String formattedTs = formatTS(formatter, ts);
                String data = formattedTs + ',' + pin + ',' + deviceName + "," + value + '\n';
                byte[] bytes = data.getBytes(UTF_8);
                baos.write(bytes, 0, bytes.length);
            }
        }
    }

    public static void writeBufToCsvFilterAndFormat(ByteArrayOutputStream baos, ByteBuffer onePinData,
                                                    String pin, long startFrom, DateTimeFormatter formatter) {

        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            if (startFrom <= ts) {
                String formattedTs = formatTS(formatter, ts);
                String data = formattedTs + ',' + pin + ',' + value + '\n';
                byte[] bytes = data.getBytes(UTF_8);
                baos.write(bytes, 0, bytes.length);
            }
        }
    }

    public static void writeBufToCsvFilterAndFormat(ByteArrayOutputStream baos, ByteBuffer onePinData,
                                                    long startFrom, DateTimeFormatter formatter) {

        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            if (startFrom <= ts) {
                String formattedTs = formatTS(formatter, ts);
                String data = formattedTs + ',' + value + '\n';
                baos.write(data.getBytes(US_ASCII), 0, data.length());
            }
        }
    }

    private static String formatTS(DateTimeFormatter formatter, long ts) {
        if (formatter == null) {
            return String.valueOf(ts);
        }
        return formatter.format(Instant.ofEpochMilli(ts));
    }

    public static void writeBufToCsv(BufferedWriter writer, ByteBuffer onePinData, int deviceId) throws Exception {
        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            writer.write("" + value + ',' + ts + ',' + deviceId + '\n');
        }
    }

    public static Path getUserReportDir(String email, String appName, int reportId, String date) {
        return Paths.get(FileUtils.CSV_DIR, email + "_" + appName + "_" + reportId + "_" + date);
    }

    public static String getUserStorageDir(String email, String appName) {
        if (AppNameUtil.BLYNK.equals(appName)) {
            return email;
        }
        return email + "_" + appName;
    }

    public static String downloadUrl(String host, String httpPort, boolean forcePort80) {
        if (forcePort80) {
            return "http://" + host + "/";
        }
        return "http://" + host + ":" + httpPort + "/";
    }

    public static File getLatestFile(File[] files) {
        if (files == null || files.length == 0) {
            return null;
        }
        File lastModifiedFile = files[0];
        for (int i = 1; i < files.length; i++) {
            if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                lastModifiedFile = files[i];
            }
        }
        return lastModifiedFile;
    }

    public static String getBuildPatternFromString(Path path) {
        return getPatternFromString(path, "\0" + "build" + "\0");
    }

    private static String getPatternFromString(Path path, String pattern) {
        try {
            byte[] data = Files.readAllBytes(path);

            int index = KMPMatch.indexOf(data, pattern.getBytes());

            if (index != -1) {
                int start = index + pattern.length();
                int end = 0;
                byte b = -1;

                while (b != '\0') {
                    end++;
                    b = data[start + end];
                }

                byte[] copy = Arrays.copyOfRange(data, start, start + end);
                return new String(copy);
            }
        } catch (Exception e) {
            log.error("Error getting pattern from file. Reason : {}", e.getMessage());
        }
        throw new RuntimeException("Unable to read build number fro firmware.");
    }

    public static Path getPathForLocalRun(String uri) {
        for (String possiblePath : POSSIBLE_LOCAL_PATHS) {
            Path path = Paths.get(possiblePath, uri);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    public static long getLastModified(Path filePath) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
        FileTime modifiedTime = attr.lastModifiedTime();
        return modifiedTime.toMillis();
    }
}

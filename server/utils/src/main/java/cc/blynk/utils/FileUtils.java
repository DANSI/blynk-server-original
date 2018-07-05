package cc.blynk.utils;

import java.io.BufferedWriter;
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

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public final class FileUtils {

    private static final String BLYNK_FOLDER = "blynk";
    public static final String CSV_DIR = Paths.get(
            System.getProperty("java.io.tmpdir"), BLYNK_FOLDER)
            .toString();

    private FileUtils() {
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
            //ignore
        }
    }

    public static void move(Path source, Path target) throws IOException {
        Path targetFile = Paths.get(target.toString(), source.getFileName().toString());
        Files.move(source, targetFile, StandardCopyOption.REPLACE_EXISTING);
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

    public static boolean writeBufToCsvFilterAndFormat(BufferedWriter writer, ByteBuffer onePinData,
                                                      String pin, String deviceName,
                                                      long startFrom, DateTimeFormatter formatter) throws IOException {
        boolean hasData = false;
        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            if (startFrom <= ts) {
                String formattedTs = formatTS(formatter, ts);
                writer.write(formattedTs + ',' + pin + ',' + deviceName + ',' + value + '\n');
                hasData = true;
            }
        }
        if (hasData) {
            writer.flush();
        }
        return hasData;
    }

    public static boolean writeBufToCsvFilterAndFormat(BufferedWriter writer, ByteBuffer onePinData, String pin,
                                                      long startFrom, DateTimeFormatter formatter) throws IOException {
        boolean hasData = false;
        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            if (startFrom <= ts) {
                String formattedTs = formatTS(formatter, ts);
                writer.write(formattedTs + ',' + pin + ',' + value + '\n');
                hasData = true;
            }
        }
        if (hasData) {
            writer.flush();
        }
        return hasData;
    }

    public static String writeBufToCsvFilterAndFormat(ByteBuffer onePinData,
                                                    long startFrom, DateTimeFormatter formatter) {
        StringBuilder sb = new StringBuilder(onePinData.capacity() * 3);
        while (onePinData.remaining() > 0) {
            double value = onePinData.getDouble();
            long ts = onePinData.getLong();

            if (startFrom <= ts) {
                String formattedTs = formatTS(formatter, ts);
                sb.append(formattedTs).append(',')
                        .append(value).append('\n');
            }
        }
        return sb.toString();
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

    public static String getPatternFromString(Path path, String pattern) throws IOException {
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

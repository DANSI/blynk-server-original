package cc.blynk.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    private final static Logger log = LogManager.getLogger(FileUtils.class);

    private FileUtils() {
    }

    public static final String BLYNK_FOLDER = "blynk";

    private static final String[] POSSIBLE_LOCAL_PATHS = new String[]{
            "./server/http-dashboard/target/classes",
            "./server/http-api/target/classes",
            "./server/http-admin/target/classes",
            "./server/http-core/target/classes",
            "./server/core/target",
            "../server/http-admin/target/classes",
            "../server/http-dashboard/target/classes",
            "../server/http-core/target/classes",
            "../server/core/target",
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
            return buf;
        }
    }

    public static String getUserReportingDir(String email, String appName) {
        if (AppNameUtil.BLYNK.equals(appName)) {
            return email;
        }
        return email + "_" + appName;
    }

    public static String csvDownloadUrl(String host, String httpPort) {
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
}

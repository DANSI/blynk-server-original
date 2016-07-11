package cc.blynk.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public class FileUtils {

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
}

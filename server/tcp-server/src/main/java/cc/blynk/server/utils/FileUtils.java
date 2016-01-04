package cc.blynk.server.utils;

import java.nio.file.Path;

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

}

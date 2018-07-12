package cc.blynk.core.http.utils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.16.
 */
public final class ContentTypeUtil {

    private ContentTypeUtil() {
    }

    public static String getContentType(String fileName) {
        if (fileName.endsWith(".ico")) {
            return "image/x-icon";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript";
        }
        if (fileName.endsWith(".css")) {
            return "text/css";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".gz")) {
            return "application/x-gzip";
        }
        if (fileName.endsWith(".zip")) {
            return "application/zip";
        }
        if (fileName.endsWith(".bin")) {
            return "application/octet-stream";
        }

        return "text/html";
    }

}

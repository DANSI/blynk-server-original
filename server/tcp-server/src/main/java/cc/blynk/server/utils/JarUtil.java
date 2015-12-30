package cc.blynk.server.utils;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.ServerLauncher;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Returns list of resources that were packed to jar
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.12.15.
 */
public class JarUtil {

    public static void unpackStaticFiles(String staticFolder) throws Exception {
        List<String> staticResources = find(staticFolder);

        for (String staticFile : staticResources) {
            try (InputStream is = ServerLauncher.class.getResourceAsStream("/" + staticFile)) {
                Path newStaticFile = ServerProperties.getFileInCurrentDir(staticFile);

                Files.deleteIfExists(newStaticFile);
                Files.createDirectories(newStaticFile);

                Files.copy(is, newStaticFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static List<String> find(String staticResourcesFolder) throws Exception {
        CodeSource src = ServerLauncher.class.getProtectionDomain().getCodeSource();
        List<String> staticResources = new ArrayList<>();

        if (src != null) {
            URL jar = src.getLocation();
            try (ZipInputStream zip = new ZipInputStream(jar.openStream())) {
                ZipEntry ze;

                while ((ze = zip.getNextEntry()) != null) {
                    String entryName = ze.getName();
                    if (entryName.startsWith(staticResourcesFolder) &&
                            (entryName.endsWith(".js") ||
                                    entryName.endsWith(".css") ||
                                    entryName.endsWith(".html")) ||
                            entryName.endsWith(".ico") ||
                            entryName.endsWith(".png")) {
                        staticResources.add(entryName);
                    }
                }
            }
        }

        return staticResources;
    }

}

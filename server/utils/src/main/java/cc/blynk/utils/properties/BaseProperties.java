package cc.blynk.utils.properties;

import cc.blynk.utils.IPUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Map;
import java.util.Properties;

import static cc.blynk.utils.properties.ServerProperties.SERVER_PROPERTIES_FILENAME;

/**
 * Java properties class wrapper.
 * Loads properties file from class path. After that loads properties
 * from dir where jar file is. On every stage properties override previous.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/12/2015.
 */
public abstract class BaseProperties extends Properties {

    public final String jarPath;

    BaseProperties(Map<String, String> cmdProperties, String serverConfig) {
        this.jarPath = getJarPath();
        String propertiesFileName = cmdProperties.get(serverConfig);
        if (propertiesFileName == null) {
            initProperties(serverConfig);
        } else {
            initProperties(Paths.get(propertiesFileName));
        }
        putAll(cmdProperties);
    }

    BaseProperties(String propertiesFileName) {
        this.jarPath = getJarPath();
        initProperties(propertiesFileName);
    }

    private static String getJarPath() {
        try {
            CodeSource codeSource = BaseProperties.class.getProtectionDomain().getCodeSource();
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            return jarFile.getParentFile().getPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * First loads properties file from class path after that from current folder.
     * So properties file in current folder is always overrides properties in classpath.
     *
     * @param filePropertiesName - name of properties file, for example "twitter4j.properties"
     */
    private void initProperties(String filePropertiesName) {
        readFromClassPath(filePropertiesName);

        Path curDirPath = Paths.get(jarPath, filePropertiesName);
        if (Files.exists(curDirPath)) {
            try (InputStream curFolder = Files.newInputStream(curDirPath)) {
                load(curFolder);
            } catch (Exception e) {
                throw new RuntimeException("Error getting properties file : " + filePropertiesName, e);
            }
        }

    }

    private void readFromClassPath(String filePropertiesName) {
        if (!filePropertiesName.startsWith("/")) {
            filePropertiesName = "/" + filePropertiesName;
        }

        try (InputStream classPath = BaseProperties.class.getResourceAsStream(filePropertiesName)) {
            if (classPath != null) {
                load(classPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting properties file : " + filePropertiesName, e);
        }
    }

    private void initProperties(Path path) {
        if (!Files.exists(path)) {
            System.out.println("Path " + path + " not found.");
            System.exit(1);
        }

        readFromClassPath(SERVER_PROPERTIES_FILENAME);

        try (InputStream curFolder = Files.newInputStream(path)) {
            load(curFolder);
        } catch (Exception e) {
            System.out.println("Error reading properties file : '" + path + "'. Reason : " + e.getMessage());
            System.exit(1);
        }
    }

    public int getIntProperty(String propertyName) {
        return Integer.parseInt(getProperty(propertyName));
    }

    public int getIntProperty(String propertyName, int defaultValue) {
        String prop = getProperty(propertyName);
        if (prop == null || prop.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(getProperty(propertyName));
    }

    public boolean isRawDBEnabled() {
        return getBoolProperty("enable.raw.db.data.store");
    }

    public boolean getBoolProperty(String propertyName) {
        return Boolean.parseBoolean(getProperty(propertyName));
    }

    public long getLongProperty(String propertyName) {
        return Long.parseLong(getProperty(propertyName));
    }

    public String getServerHost() {
        final String host = getProperty("server.host");
        if (host == null || host.isEmpty()) {
            final String netInterface = getProperty("net.interface", "eth");
            return IPUtils.resolveHostIP(netInterface);
        } else {
            return host;
        }
    }

    public long getLongProperty(String propertyName, long defaultValue) {
        String prop = getProperty(propertyName);
        if (prop == null || prop.isEmpty()) {
            return defaultValue;
        }
        return Long.parseLong(prop);
    }

    public String[] getCommaSeparatedValueAsArray(String propertyName) {
        String val = getProperty(propertyName);
        if (val == null) {
            return null;
        }
        return val.toLowerCase().split(",");
    }

}

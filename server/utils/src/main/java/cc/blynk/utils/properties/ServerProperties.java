package cc.blynk.utils.properties;

import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.JarUtil;

import java.util.Map;

/**
 * Java properties class wrapper.
 * Loads properties file from class path. After that loads properties
 * from dir where jar file is. On every stage properties override previous.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/12/2015.
 */
public class ServerProperties extends BaseProperties {

    public static final String SERVER_PROPERTIES_FILENAME = "server.properties";
    public static final String PRODUCT_NAME = "{PRODUCT_NAME}";
    public static final String DEVICE_NAME = "{DEVICE_NAME}";

    private static final String STATIC_FILES_FOLDER = "static";

    public final boolean isUnpacked;

    public ServerProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, SERVER_PROPERTIES_FILENAME);
        this.isUnpacked = JarUtil.unpackStaticFiles(jarPath, STATIC_FILES_FOLDER);
    }

    public ServerProperties(String propertiesFileName) {
        super(propertiesFileName);
        this.isUnpacked = JarUtil.unpackStaticFiles(jarPath, STATIC_FILES_FOLDER);
    }

    public String getProductName() {
        return getProperty("product.name", AppNameUtil.BLYNK);
    }

    public String getAdminUrl(String host) {
        String httpsPort = getHttpsPortOrBlankIfDefaultAsString();
        return "https://" + host + httpsPort + getAdminRootPath();
    }

    private String getHttpsPortOrBlankIfDefaultAsString() {
        if (force80Port()) {
            //means default port 443 is used, so no need to attach it
            return "";
        }
        String httpsPort = getProperty("https.port");
        if (httpsPort == null || httpsPort.equals("443")) {
            return "";
        }
        return ":" + httpsPort;
    }

    public String getHttpsPortAsString() {
        return force80Port() ? "443" : getProperty("https.port");
    }

    public boolean force80Port() {
        return getBoolProperty("force.port.80.for.csv");
    }

    public boolean getAllowStoreIp() {
        return getBoolProperty("allow.store.ip");
    }

    public boolean renameOldReportingFiles() {
        return getBoolProperty("rename.old.reporting.files");
    }
}

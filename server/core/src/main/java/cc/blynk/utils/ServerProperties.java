package cc.blynk.utils;

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

    public static final String STATIC_FILES_FOLDER = "static";

    public final boolean isUnpacked;

    public ServerProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, SERVER_PROPERTIES_FILENAME);
        this.isUnpacked = JarUtil.unpackStaticFiles(jarPath, STATIC_FILES_FOLDER);
    }

    public ServerProperties(String propertiesFileName) {
        super(propertiesFileName);
        this.isUnpacked = JarUtil.unpackStaticFiles(jarPath, STATIC_FILES_FOLDER);
    }

}

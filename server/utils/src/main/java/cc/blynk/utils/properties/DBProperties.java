package cc.blynk.utils.properties;

import java.util.Collections;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.01.17.
 */
public class DBProperties extends BaseProperties {

    public static final String DB_PROPERTIES_FILENAME = "db.properties";

    public DBProperties(String fileName) {
        super(Collections.emptyMap(), fileName);
    }

    public DBProperties() {
        super(Collections.emptyMap(), DB_PROPERTIES_FILENAME);
    }

    public boolean cleanReporting() {
        return getBoolProperty("clean.reporting");
    }
}

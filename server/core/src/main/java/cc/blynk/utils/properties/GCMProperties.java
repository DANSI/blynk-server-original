package cc.blynk.utils.properties;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.01.17.
 */
public class GCMProperties extends BaseProperties {

    public static final String GCM_PROPERTIES_FILENAME = "gcm.properties";

    public GCMProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, GCM_PROPERTIES_FILENAME);
    }
}

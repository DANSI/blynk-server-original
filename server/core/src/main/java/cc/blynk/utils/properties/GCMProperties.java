package cc.blynk.utils.properties;

import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.utils.BaseProperties;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.01.17.
 */
public class GCMProperties extends BaseProperties {

    public GCMProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, GCMWrapper.GCM_PROPERTIES_FILENAME);
    }
}

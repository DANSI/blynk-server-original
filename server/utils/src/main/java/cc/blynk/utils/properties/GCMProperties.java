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

    public String getNotificationTitle() {
        return getProperty("notification.title", Placeholders.PRODUCT_NAME + " Notification");
    }

    public String getNotificationBody() {
        return getProperty("notification.body", "Your " + Placeholders.DEVICE_NAME + " went offline.");
    }

    public String getGCMApiKey() {
        return getProperty("gcm.api.key");
    }

    public String getGCMServer() {
        return getProperty("gcm.server");
    }
}

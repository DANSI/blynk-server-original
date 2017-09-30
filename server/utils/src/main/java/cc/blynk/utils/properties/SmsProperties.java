package cc.blynk.utils.properties;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.01.17.
 */
public class SmsProperties extends BaseProperties {

    public static final String SMS_PROPERTIES_FILENAME = "sms.properties";

    public SmsProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, SMS_PROPERTIES_FILENAME);
    }
}

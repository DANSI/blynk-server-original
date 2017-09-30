package cc.blynk.utils.properties;

import cc.blynk.server.notifications.sms.SMSWrapper;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.01.17.
 */
public class SmsProperties extends BaseProperties {

    public SmsProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, SMSWrapper.SMS_PROPERTIES_FILENAME);
    }
}

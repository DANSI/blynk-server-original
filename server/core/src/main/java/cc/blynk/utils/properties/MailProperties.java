package cc.blynk.utils.properties;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.01.17.
 */
public class MailProperties extends BaseProperties {

    public static final String MAIL_PROPERTIES_FILENAME = "mail.properties";

    public MailProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, MAIL_PROPERTIES_FILENAME);
    }
}

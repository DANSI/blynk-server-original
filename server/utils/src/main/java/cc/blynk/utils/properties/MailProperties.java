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

    public String getSMTPUsername() {
        return getProperty("mail.smtp.username");
    }

    public String getSMTPPassword() {
        return getProperty("mail.smtp.password");
    }

    public String getSMTPHost() {
        return getProperty("mail.smtp.host");
    }

    public String getSMTPort() {
        return getProperty("mail.smtp.port");
    }
}

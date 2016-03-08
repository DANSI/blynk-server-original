package cc.blynk.server.notifications.mail;

import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.04.15.
 */
public class MailWrapperTest {

    @Test
    @Ignore
    public void sendMail() throws Exception {
        Properties properties = new Properties();
        try (InputStream classPath = MailWrapperTest.class.getResourceAsStream("/mail.properties")) {
            if (classPath != null) {
                properties.load(classPath);
            }
        }

        String to = "";
        MailWrapper mailWrapper = new MailWrapper(properties);
        mailWrapper.send(to, "Hello", "Body!");
    }

}

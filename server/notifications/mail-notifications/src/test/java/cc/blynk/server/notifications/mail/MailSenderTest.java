package cc.blynk.server.notifications.mail;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.04.15.
 */
public class MailSenderTest {

    @Test
    @Ignore
    public void sendMail() throws IOException {
        Properties properties = new Properties();
        try (InputStream classPath = MailSenderTest.class.getResourceAsStream("/mail.properties")) {
            if (classPath != null) {
                properties.load(classPath);
            }
        }

        String to = "";
        MailSender mailSender = new MailSender(properties);
        mailSender.produce(to, "Hello", "Body!").run();
    }

}

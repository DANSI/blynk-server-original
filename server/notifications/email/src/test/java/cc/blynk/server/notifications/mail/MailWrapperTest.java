package cc.blynk.server.notifications.mail;

import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    @Ignore
    public void sendMaiWithAttachments() throws Exception {
        Properties properties = new Properties();
        try (InputStream classPath = MailWrapperTest.class.getResourceAsStream("/mail.properties")) {
            if (classPath != null) {
                properties.load(classPath);
            }
        }

        String to = "doom369@gmail.com";
        MailWrapper mailWrapper = new MailWrapper(properties);
        List<Path> attachments = new ArrayList<Path>() {
            {
                add(Paths.get("/home/doom369/clone-qr/123.csv.gz"));
                add(Paths.get("/home/doom369/clone-qr/123_2.csv.gz"));
            }
        };
        mailWrapper.send(to, "Hello", "Body!", attachments);
    }

}

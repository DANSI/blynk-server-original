package cc.blynk.server.notifications.mail;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
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

    private DefaultAsyncHttpClient httpclient = new DefaultAsyncHttpClient(
            new DefaultAsyncHttpClientConfig.Builder()
    .setUserAgent(null)
    .setKeepAlive(false)
    .build()
    );

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
        MailWrapper mailWrapper = new MailWrapper(properties, httpclient);
        mailWrapper.sendText(to, "Hello", "Body!");
    }

    @Test
    @Ignore
    public void sendMailWithAttachments() throws Exception {
        Properties properties = new Properties();
        try (InputStream classPath = MailWrapperTest.class.getResourceAsStream("/mail.properties")) {
            if (classPath != null) {
                properties.load(classPath);
            }
        }

        String to = "doom369@gmail.com";
        MailWrapper mailWrapper = new MailWrapper(properties, httpclient);

        mailWrapper.sendText(to, "Hello", "Body!");
    }

    @Test
    @Ignore
    public void sendMailWithHttpProvider() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("mail.host", "");
        properties.setProperty("mail.api.key", "");
        properties.setProperty("mail.from", "");

        String to = "";
        MailWrapper mailWrapper = new MailWrapper(properties, httpclient);

        mailWrapper.sendText(to, "Hello", "Body!");
    }

}

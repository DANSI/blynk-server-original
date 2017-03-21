package cc.blynk.server.notifications.mail;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public void sendMailWithAttachments() throws Exception {
        Properties properties = new Properties();
        try (InputStream classPath = MailWrapperTest.class.getResourceAsStream("/mail.properties")) {
            if (classPath != null) {
                properties.load(classPath);
            }
        }

        QrHolder qrHolder = new QrHolder("123.jpg", QRCode.from("123").to(ImageType.JPG).stream().toByteArray());
        QrHolder qrHolder2 = new QrHolder("124.jpg", QRCode.from("124").to(ImageType.JPG).stream().toByteArray());

        String to = "doom369@gmail.com";
        MailWrapper mailWrapper = new MailWrapper(properties);
        mailWrapper.sendHtmlWithAttachment(to, "Hello", "Body!", new QrHolder[] {qrHolder, qrHolder2});
    }

    private static void generateQR(String text, Path outputFile) throws Exception {
        try (OutputStream out = Files.newOutputStream(outputFile)) {
            QRCode.from(text).to(ImageType.JPG).writeTo(out);
        }
    }


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
        mailWrapper.sendText(to, "Hello", "Body!");
    }

    @Test
    @Ignore
    public void sendMail2() throws Exception {
        Properties properties = new Properties();
        try (InputStream classPath = MailWrapperTest.class.getResourceAsStream("/mail.properties")) {
            if (classPath != null) {
                properties.load(classPath);
            }
        }

        String to = "doom369@gmail.com";
        MailWrapper mailWrapper = new MailWrapper(properties);

        mailWrapper.sendText(to, "Hello", "Body!");
    }

    @Test
    @Ignore
    public void sendMailWithHttpProvider() throws Exception {
        Properties properties = new Properties();
        try (InputStream classPath = MailWrapperTest.class.getResourceAsStream("/mail.properties")) {
            if (classPath != null) {
                properties.load(classPath);
            }
        }

        String to = "";

        MailWrapper mailWrapper = new MailWrapper(properties);

        mailWrapper.sendText(to, "Hello", "Happy Blynking!\n" +
                "-\n" +
                "Getting Started Guide -> http://www.blynk.cc/getting-started\n" +
                "Documentation -> http://docs.blynk.cc/\n" +
                "Sketch generator -> http://examples.blynk.cc/\n" +
                "\n" +
                "Latest Blynk library -> https://github.com/blynkkk/blynk-library/releases/download/v0.3.9/Blynk_v0.3.9.zip\n" +
                "Latest Blynk server -> https://github.com/blynkkk/blynk-server/releases/download/v0.18.1/server-0.18.1.jar\n" +
                "-\n" +
                "http://www.blynk.cc\n" +
                "twitter.com/blynk_app\n" +
                "www.facebook.com/blynkapp");
    }

}

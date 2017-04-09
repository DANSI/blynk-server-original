package cc.blynk.server.notifications.mail;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
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
    public void sendMailForStaticProvisioning() throws Exception {
        String body =
                "Hi there,<br>\n" +
                        "<br>\n" +
                        "Nice app you made with Blynk!<br>\n" +
                        "<br>\n" +
                        "Here is what's next:\n" +
                        "\n" +
                        "<ul>\n" +
                        "    <li>For Static Provisioning you need to upload Auth Tokens provided in this email to your devices. Tokens are in the attachment.</li>\n" +
                        "\n" +
                        "    <li>During the provisioning process, device will be connected to your network. You need to scan provided QRs in order to connect your app to devices. Learn <a href=\"http://help.blynk.cc/publishing-apps-made-with-blynk/1240196-provisioning-products-with-auth-tokens/static-auth-token-provisioning\">how Static Device Provisioning works</a>.</li>\n" +
                        "</ul>\n" +
                        "\n" +
                        "<b>If you would like to publish your app to App Store and Google Play, check out our <a href=\"http://www.blynk.io/plans/\">plans</a> and send a request.</b><br>\n" +
                        "<br>\n" +
                        "Let’s build a connected world together!<br>\n" +
                        "<br>\n" +
                        "--<br>\n" +
                        "<br>\n" +
                        "Blynk Team<br>\n" +
                        "<br>\n" +
                        "<a href=\"http://www.blynk.io\">blynk.io</a>\n" +
                        "<br>\n" +
                        "<a href=\"http://www.blynk.cc\">blynk.cc</a>";
        QrHolder[] qrHolders = new QrHolder[] {
                new QrHolder("!23", "123", QRCode.from("21321321").to(ImageType.JPG).stream().toByteArray())
        };

        Properties properties = new Properties();
        try (InputStream classPath = MailWrapperTest.class.getResourceAsStream("/mail.properties")) {
            if (classPath != null) {
                properties.load(classPath);
            }
        }

        MailWrapper mailWrapper = new MailWrapper(properties);
        mailWrapper.sendWithAttachment("dmitriy@gmail.com", "yo", body, qrHolders);
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

        QrHolder qrHolder = new QrHolder("123.jpg", "123", QRCode.from("123").to(ImageType.JPG).stream().toByteArray());
        QrHolder qrHolder2 = new QrHolder("124.jpg", "123",  QRCode.from("124").to(ImageType.JPG).stream().toByteArray());

        String to = "doom369@gmail.com";
        MailWrapper mailWrapper = new MailWrapper(properties);
        mailWrapper.sendWithAttachment(to, "Hello", "Body!", new QrHolder[]{qrHolder, qrHolder2});
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

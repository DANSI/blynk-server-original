package cc.blynk.server.notifications.mail;

import cc.blynk.server.notifications.mail.http.HttpMailClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;

import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.04.15.
 */
public class MailWrapper {

    public static final String MAIL_PROPERTIES_FILENAME = "mail.properties";

    private static final Logger log = LogManager.getLogger(MailWrapper.class);
    private final MailClient client;

    public MailWrapper(Properties mailProperties, AsyncHttpClient asyncHttpClient) {
        String apiKey = mailProperties.getProperty("mail.api_key");
        if (apiKey == null || apiKey.equals("")) {
            client = new SmtpMailClient(mailProperties);
        } else {
            client = new HttpMailClient(mailProperties, asyncHttpClient);
        }
    }

    public void sendText(String to, String subj, String body) throws Exception {
        client.sendText(to, subj, body);
    }

    public void sendHtml(String to, String subj, String body) throws Exception {
        client.sendHtml(to, subj, body);
    }

}

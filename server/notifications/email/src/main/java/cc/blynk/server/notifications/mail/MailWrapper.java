package cc.blynk.server.notifications.mail;

import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.04.15.
 */
public class MailWrapper {

    private final MailClient client;

    public MailWrapper(Properties mailProperties) {
        String host = mailProperties.getProperty("mail.smtp.host");
        if (host != null && (host.contains("sparkpostmail") || host.contains("amazonaws.com"))) {
            // Amazon AWS Simple Email Service uses an account (mail.from) distinct from the username,
            // which is just like SparkPost.
            client = new SparkPostMailClient(mailProperties);
        } else {
            client = new GMailClient(mailProperties);
        }
    }

    public void sendText(String to, String subj, String body) throws Exception {
        client.sendText(to, subj, body);
    }

    public void sendHtml(String to, String subj, String body) throws Exception {
        client.sendHtml(to, subj, body);
    }

    public void sendWithAttachment(String to, String subj, String body, QrHolder[] attachments) throws Exception {
        client.sendHtmlWithAttachment(to, subj, body, attachments);
    }

}

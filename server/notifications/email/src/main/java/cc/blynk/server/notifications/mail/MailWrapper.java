package cc.blynk.server.notifications.mail;

import cc.blynk.utils.FileLoaderUtil;
import cc.blynk.utils.properties.MailProperties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.04.15.
 */
public class MailWrapper {

    private final MailClient client;
    private final String emailBody;

    public MailWrapper(MailProperties mailProperties) {
        String host = mailProperties.getProperty("mail.smtp.host");
        if (host != null && host.contains("sparkpostmail")) {
            client = new SparkPostMailClient(mailProperties);
        } else {
            client = new GMailClient(mailProperties);
        }
        this.emailBody = FileLoaderUtil.readFileAsString("static/register-email.html");
    }

    public void sendWelcomeEmailForNewUser(String to) throws Exception {
        sendHtml(to, "Get started with Blynk", emailBody);
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

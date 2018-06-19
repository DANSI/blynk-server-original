package cc.blynk.server.notifications.mail;

import cc.blynk.utils.FileLoaderUtil;
import cc.blynk.utils.properties.MailProperties;
import cc.blynk.utils.properties.Placeholders;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.04.15.
 */
public class MailWrapper {

    private final MailClient client;
    private final String emailBody;
    private final String reportBody;
    private final String customerEmail;

    public MailWrapper(MailProperties mailProperties, String productName, String customerEmail) {
        String host = mailProperties.getProperty("mail.smtp.host");
        if (host != null && host.contains("sparkpostmail")) {
            client = new SparkPostMailClient(mailProperties, productName);
        } else {
            client = new GMailClient(mailProperties);
        }
        this.emailBody = FileLoaderUtil.readFileAsString("static/register-email.html");
        this.reportBody = FileLoaderUtil.readFileAsString("static/report-email.html");
        this.customerEmail = customerEmail;
    }

    public void sendReportEmail(String to,
                                String subj,
                                String downloadUrl,
                                String dynamicSection) throws Exception  {
        String body = reportBody
                .replace(Placeholders.DOWNLOAD_URL, downloadUrl)
                .replace(Placeholders.DYNAMIC_SECTION, dynamicSection);
        sendHtml(to, subj, body);
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

    public void sendWithAttachment(String to, String subj, String body, QrHolder attachment) throws Exception {
        client.sendHtmlWithAttachment(to, subj, body, new QrHolder[] {attachment});
    }

    public void sendWithAttachment(String to, String subj, String body, QrHolder[] attachments) throws Exception {
        client.sendHtmlWithAttachment(to, subj, body, attachments);
    }

}

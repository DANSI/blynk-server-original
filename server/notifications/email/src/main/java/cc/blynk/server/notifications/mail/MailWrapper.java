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
    private final String reportBody;
    private final String productName;

    public MailWrapper(MailProperties mailProperties, String productName) {
        String host = mailProperties.getProperty("mail.smtp.host");
        if (host != null) {
            mailProperties.put("mail.smtp.ssl.trust", host);
        }
        if (host != null && (host.endsWith("sparkpostmail.com") || host.endsWith("amazonaws.com"))) {
            // Amazon AWS Simple Email Service uses an account (mail.from) distinct from the username,
            // which is just like SparkPost.
            client = new ThirdPartyMailClient(mailProperties, productName);
        } else {
            client = new GMailClient(mailProperties);
        }
        this.reportBody = FileLoaderUtil.readReportEmailTemplate();
        this.productName = productName;
    }

    public void sendReportEmail(String to,
                                String subj,
                                String downloadUrl,
                                String dynamicSection) throws Exception  {
        String body = reportBody
                .replace(Placeholders.DOWNLOAD_URL, downloadUrl)
                .replace(Placeholders.DYNAMIC_SECTION, dynamicSection)
                .replace(Placeholders.PRODUCT_NAME, productName);
        sendHtml(to, subj, body);
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

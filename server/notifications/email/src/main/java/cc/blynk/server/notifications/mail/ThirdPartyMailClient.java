package cc.blynk.server.notifications.mail;

import cc.blynk.utils.properties.MailProperties;
import cc.blynk.utils.properties.Placeholders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.09.16.
 */
public class ThirdPartyMailClient implements MailClient {

    private static final Logger log = LogManager.getLogger(ThirdPartyMailClient.class);

    private final Session session;
    private final InternetAddress from;
    private final String host;
    private final String username;
    private final String password;

    ThirdPartyMailClient(MailProperties mailProperties, String productName) {
        this.username = mailProperties.getSMTPUsername();
        this.password = mailProperties.getSMTPPassword();
        this.host = mailProperties.getSMTPHost();

        log.info("Initializing SparkPost smtp mail transport. Username : {}. SMTP host : {}:{}",
                username, host, mailProperties.getSMTPort());

        this.session = Session.getInstance(mailProperties);
        try {
            String mailFrom = mailProperties.getProperty("mail.from");
            if (mailFrom != null) {
                mailFrom = mailFrom.replace(Placeholders.PRODUCT_NAME, productName);
            }
            this.from = new InternetAddress(mailFrom);
        } catch (AddressException e) {
            throw new RuntimeException("Error initializing MailWrapper.");
        }
    }

    @Override
    public void sendText(String to, String subj, String body) throws Exception {
        send(to, subj, body, TEXT_PLAIN_CHARSET_UTF_8);
    }

    @Override
    public void sendHtml(String to, String subj, String body) throws Exception {
        send(to, subj, body, TEXT_HTML_CHARSET_UTF_8);
    }

    private void send(String to, String subj, String body, String contentType) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subj, "UTF-8");
        message.setContent(body, contentType);

        try (Transport transport = session.getTransport()) {
            transport.connect(host, username, password);
            transport.sendMessage(message, message.getAllRecipients());
        }

        log.debug("Mail sent to {}. Subj: {}", to, subj);
        log.trace("Mail body: {}", body);
    }

    @Override
    public void sendHtmlWithAttachment(String to, String subj, String body, QrHolder[] attachments) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subj, "UTF-8");

        Multipart multipart = new MimeMultipart();

        MimeBodyPart bodyMessagePart = new MimeBodyPart();
        bodyMessagePart.setContent(body, TEXT_HTML_CHARSET_UTF_8);
        multipart.addBodyPart(bodyMessagePart);

        for (QrHolder qrHolder : attachments) {
            MimeBodyPart attachmentsPart = new MimeBodyPart();
            attachmentsPart.setDataHandler(new DataHandler(new ByteArrayDataSource(qrHolder.data, "image/jpeg")));
            attachmentsPart.setFileName(qrHolder.makeQRFilename());
            multipart.addBodyPart(attachmentsPart);
        }

        message.setContent(multipart);

        try (Transport transport = session.getTransport()) {
            transport.connect(host, username, password);
            transport.sendMessage(message, message.getAllRecipients());
        }

        log.debug("Mail sent to {}. Subj: {}", to, subj);
        log.trace("Mail body: {}", body);
    }

}

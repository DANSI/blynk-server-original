package cc.blynk.server.notifications.mail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.09.16.
 */
public class GMailClient implements MailClient {

    private static final Logger log = LogManager.getLogger(MailWrapper.class);

    private final Session session;
    private final InternetAddress from;

    public GMailClient(Properties mailProperties) {
        final String username = mailProperties.getProperty("mail.smtp.username");
        final String password = mailProperties.getProperty("mail.smtp.password");

        log.info("Initializing gmail smtp mail transport. Username : {}. SMTP host : {}:{}",
                username, mailProperties.getProperty("mail.smtp.host"), mailProperties.getProperty("mail.smtp.port"));

        this.session = Session.getInstance(mailProperties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            this.from = new InternetAddress(username);
        } catch (AddressException e) {
            throw new RuntimeException("Error initializing MailWrapper." + e.getMessage());
        }
    }

    @Override
    public void sendText(String to, String subj, String body) throws Exception {
        send(to, subj, body, "text/plain; charset=UTF-8");
    }

    @Override
    public void sendHtml(String to, String subj, String body) throws Exception {
        send(to, subj, body, "text/html");
    }

    @Override
    public void sendHtmlWithAttachment(String to, String subj, String body, QrHolder[] attachments) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subj, "UTF-8");

        Multipart multipart = new MimeMultipart();

        MimeBodyPart bodyMessagePart = new MimeBodyPart();
        bodyMessagePart.setText(body);
        bodyMessagePart.setContent(body, "text/html");

        multipart.addBodyPart(bodyMessagePart);

        for (QrHolder qrHolder : attachments) {
            MimeBodyPart attachmentsPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(qrHolder.data, "image/jpeg");
            attachmentsPart.setDataHandler(new DataHandler(source));
            attachmentsPart.setFileName(qrHolder.name);
            multipart.addBodyPart(attachmentsPart);
        }

        message.setContent(multipart);

        Transport.send(message);

        log.trace("Mail to {} was sent. Subj : {}, body : {}", to, subj, body);
    }

    private void send(String to, String subj, String body, String contentType) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subj, "UTF-8");
        message.setContent(body, contentType);

        Transport.send(message);
        log.trace("Mail to {} was sent. Subj : {}, body : {}", to, subj, body);
    }

}

package cc.blynk.server.notifications.mail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.09.16.
 */
public class SparkPostMailClient implements MailClient {

    private static final Logger log = LogManager.getLogger(SparkPostMailClient.class);

    private final Session session;
    private final InternetAddress from;
    private final String host;
    private final String username;
    private final String password;

    public SparkPostMailClient(Properties mailProperties) {
        this.username = mailProperties.getProperty("mail.smtp.username");
        this.password = mailProperties.getProperty("mail.smtp.password");
        this.host = mailProperties.getProperty("mail.smtp.host");

        log.info("Initializing SparkPost smtp mail transport. Username : {}. SMTP host : {}:{}",
                username, host, mailProperties.getProperty("mail.smtp.port"));

        this.session = Session.getInstance(mailProperties);
        try {
            this.from = new InternetAddress(mailProperties.getProperty("mail.from"));
        } catch (AddressException e) {
            throw new RuntimeException("Error initializing MailWrapper.");
        }
    }

    @Override
    public void sendText(String to, String subj, String body) throws Exception {
        send(to, subj, body, "text/plain; charset=UTF-8");
    }

    @Override
    public void sendHtml(String to, String subj, String body) throws Exception {
        send(to, subj, body, "text/html; charset=UTF-8");
    }

    private void send(String to, String subj, String body, String contentType) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subj, "UTF-8");
        message.setContent(body, contentType);

        Transport transport = session.getTransport();
        try {
            transport.connect(host, username, password);
            transport.sendMessage(message, message.getAllRecipients());
        } finally {
            transport.close();
        }

        log.trace("Mail to {} was sent. Subj : {}, body : {}", to, subj, body);
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
            attachmentsPart.setDataHandler(new DataHandler(new ByteArrayDataSource(qrHolder.data, "image/jpeg")));
            attachmentsPart.setFileName(qrHolder.name);
            multipart.addBodyPart(attachmentsPart);
        }

        message.setContent(multipart);

        Transport transport = session.getTransport();
        try {
            transport.connect(host, username, password);
            transport.sendMessage(message, message.getAllRecipients());
        } finally {
            transport.close();
        }

        log.trace("Mail to {} was sent. Subj : {}, body : {}", to, subj, body);
    }

}

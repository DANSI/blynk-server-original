package cc.blynk.server.notifications.mail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.04.15.
 */
public class MailWrapper {

    public static final String MAIL_PROPERTIES_FILENAME = "mail.properties";

    private static final Logger log = LogManager.getLogger(MailWrapper.class);

    private final Session session;
    private final InternetAddress from;

    public MailWrapper(Properties mailProperties) {
        final String username = mailProperties.getProperty("mail.smtp.username");
        final String password = mailProperties.getProperty("mail.smtp.password");

        log.info("Initializing mail transport. Username : {}. SMTP host : {}:{}",
                username, mailProperties.getProperty("mail.smtp.host"), mailProperties.getProperty("mail.smtp.port"));

        this.session = Session.getInstance(mailProperties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            this.from = new InternetAddress(username);
        } catch (AddressException e) {
            throw new RuntimeException("Error initializing MailWrapper.");
        }
    }

    public void send(String to, String subj, String body) throws MessagingException {
        send(to, subj, body, "");
    }

    public void send(String to, String subj, String body, String contentType) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subj);
        if ("".equals(contentType)) {
            message.setText(body);
        } else {
            message.setContent(body, contentType);
        }

        Transport.send(message);
        log.trace("Mail to {} was sent. Subj : {}, body : {}", to, subj, body);
    }

    public void send(String to, String subj, String body, Path attachment) throws MessagingException {
        send(to, subj, body, Collections.singletonList(attachment));
    }

    public void send(String to, String subj, String body, List<Path> attachments) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subj);
        message.setText(body);

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        Multipart multipart = new MimeMultipart();

        for (Path path : attachments) {
            DataSource source = new FileDataSource(path.toString());
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(path.getFileName().toString());
            multipart.addBodyPart(messageBodyPart);
        }

        message.setContent(multipart);

        Transport.send(message);
        log.trace("Mail to {} was sent. Subj : {}, body : {}", to, subj, body);
    }

}

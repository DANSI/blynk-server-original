package cc.blynk.server.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.04.15.
 */
public class MailSender {

    private static final Logger log = LogManager.getLogger(MailSender.class);

    private final Session session;
    private final InternetAddress from;

    public MailSender(Properties mailProperties) throws AddressException {
        String username = mailProperties.getProperty("mail.smtp.username");
        String password = mailProperties.getProperty("mail.smtp.password");

        log.info("Initializing mail transport. Username : {}. SMTP host : {}:{}",
                username, mailProperties.getProperty("mail.smtp.host"), mailProperties.getProperty("mail.smtp.port"));

        this.session = Session.getInstance(mailProperties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        this.from = new InternetAddress(username);
    }

    public void send(String to, String subj, String body) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom();
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subj);
        message.setText(body);

        Transport.send(message);

        log.debug("Mail from {} to {} was sent. Subj : {}, body : {}", from, to, subj, body);
    }

}

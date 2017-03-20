package cc.blynk.server.notifications.mail;

import java.nio.file.Path;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.09.16.
 */
public interface MailClient {

    void sendText(String to, String subj, String body) throws Exception;

    void sendHtml(String to, String subj, String body) throws Exception;

    void sendHtmlWithAttachment(String to, String subj, String body, Path[] attachments) throws Exception;

}

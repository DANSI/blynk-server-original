package cc.blynk.server.notifications.mail;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.09.16.
 */
public interface MailClient {

    String TEXT_PLAIN_CHARSET_UTF_8 = "text/plain; charset=UTF-8";
    String TEXT_HTML_CHARSET_UTF_8 = "text/html; charset=UTF-8";

    void sendText(String to, String subj, String body) throws Exception;

    void sendHtml(String to, String subj, String body) throws Exception;

    void sendHtmlWithAttachment(String to, String subj, String body, QrHolder[] attachments) throws Exception;

}

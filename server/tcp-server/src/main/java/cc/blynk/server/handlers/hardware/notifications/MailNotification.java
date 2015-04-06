package cc.blynk.server.handlers.hardware.notifications;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
public class MailNotification extends NotificationBase {

    public String to;

    public String subj;

    public MailNotification(String to, String subj, String body, int msgId) {
        super(body, msgId);
        this.to = to;
        this.subj = subj;
    }
}

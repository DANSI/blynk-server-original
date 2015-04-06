package cc.blynk.server.handlers.hardware.notifications;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
public abstract class NotificationBase {

    public String body;

    public int msgId;

    public NotificationBase(String body, int msgId) {
        this.body = body;
        this.msgId = msgId;
    }
}

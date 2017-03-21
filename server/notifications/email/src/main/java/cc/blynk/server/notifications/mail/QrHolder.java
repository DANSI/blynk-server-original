package cc.blynk.server.notifications.mail;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.17.
 */
public class QrHolder {

    public final String name;

    public final byte[] data;

    public QrHolder(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }
}

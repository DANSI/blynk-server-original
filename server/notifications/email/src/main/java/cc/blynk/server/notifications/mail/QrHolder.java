package cc.blynk.server.notifications.mail;

import java.util.Arrays;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.17.
 */
public class QrHolder {

    public final String name;

    public final String mailBodyPart;

    public final byte[] data;

    public QrHolder(String name, String mailBodyPart, byte[] data) {
        this.name = name;
        this.mailBodyPart = mailBodyPart;
        this.data = data;
    }

    //for tests only
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QrHolder)) return false;

        QrHolder qrHolder = (QrHolder) o;

        if (name != null ? !name.equals(qrHolder.name) : qrHolder.name != null) return false;
        return Arrays.equals(data, qrHolder.data);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
        return result;
    }
}

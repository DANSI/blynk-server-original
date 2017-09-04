package cc.blynk.server.notifications.mail;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.17.
 */
public class QrHolder {

    public final int dashId;

    public final int deviceId;

    public final String deviceName;

    public final String token;

    public final byte[] data;

    public QrHolder(int dashId, int deviceId, String deviceName, String token, byte[] data) {
        this.dashId = dashId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.token = token;
        this.data = data;
    }

    String makeQRFilename() {
        return token + "_" + dashId + "_" + deviceId + ".jpg";
    }

    public void attach(StringBuilder sb) {
        sb.append("<br>")
                .append(deviceName)
                .append(": ")
                .append(token);
    }

    //for tests only
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QrHolder)) {
            return false;
        }

        QrHolder qrHolder = (QrHolder) o;

        if (dashId != qrHolder.dashId) {
            return false;
        }
        if (deviceId != qrHolder.deviceId) {
            return false;
        }
        return !(token != null ? !token.equals(qrHolder.token) : qrHolder.token != null);

    }

    @Override
    public int hashCode() {
        int result = dashId;
        result = 31 * result + deviceId;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        return result;
    }
}

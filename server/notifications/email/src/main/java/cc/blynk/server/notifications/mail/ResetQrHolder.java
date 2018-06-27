package cc.blynk.server.notifications.mail;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.17.
 */
public class ResetQrHolder extends QrHolder {

    public ResetQrHolder(String token, byte[] data) {
        super(-1, -1, null, token, data);
    }

    @Override
    String makeQRFilename() {
        return token;
    }

}

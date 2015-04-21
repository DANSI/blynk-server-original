package cc.blynk.server.handlers.administration;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.04.15.
 */
public class AdminMessage {

    public byte[] classBytes;

    public String[] params;

    public AdminMessage(byte[] classBytes, String[] params) {
        this.classBytes = classBytes;
        this.params = params;
    }
}

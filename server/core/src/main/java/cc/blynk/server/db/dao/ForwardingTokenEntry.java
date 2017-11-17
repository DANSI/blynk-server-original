package cc.blynk.server.db.dao;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 17.11.17.
 */
public class ForwardingTokenEntry {

    public final String token;
    public final String host;
    public final String email;
    public final int dashId;
    public final int deviceId;

    public ForwardingTokenEntry(String token, String host, String email, int dashId, int deviceId) {
        this.token = token;
        this.host = host;
        this.email = email;
        this.dashId = dashId;
        this.deviceId = deviceId;
    }
}

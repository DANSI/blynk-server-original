package cc.blynk.server.internal;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
public final class TokenUser {

    public final String email;
    public final String appName;
    public final long createdAt;

    public TokenUser(String email, String appName) {
        this.email = email;
        this.appName = appName;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "TokenUser{" +
                "email='" + email + '\'' +
                ", appName='" + appName + '\'' +
                '}';
    }
}

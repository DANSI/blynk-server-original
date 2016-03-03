package cc.blynk.server.db;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.03.16.
 */
public class Redeem {

    public String token;

    public String company;

    public boolean isRedeemed;

    public String username;

    public int version;

    public Redeem() {
    }

    public Redeem(String token, String company, boolean isRedeemed, String username, int version) {
        this.token = token;
        this.company = company;
        this.isRedeemed = isRedeemed;
        this.username = username;
        this.version = version;
    }
}

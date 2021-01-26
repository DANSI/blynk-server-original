package cc.blynk.server.db.model;

import java.util.Date;
import java.util.StringJoiner;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.03.16.
 */
public class Redeem {

    public String token;

    public String company;

    public final boolean isRedeemed;

    public String email;

    public int reward;

    public final int version;

    public Date ts;

    public String formatToken() {
        return new StringJoiner("+")
                .add(company)
                .add(String.valueOf(reward))
                .add(token).toString();
    }

    public String formatToken(String title, String text) {
        return new StringJoiner("+")
                .add(company)
                .add(String.valueOf(reward))
                .add(token)
                .add(title)
                .add(text)
                .toString();
    }

    public Redeem() {
        this.isRedeemed = false;
        this.version = 1;
    }

    public Redeem(String token, String company, int reward) {
        this();
        this.token = token;
        this.company = company;
        this.reward = reward;
    }

    public Redeem(String token, String company, boolean isRedeemed, String email, int reward, int version, Date ts) {
        this.token = token;
        this.company = company;
        this.isRedeemed = isRedeemed;
        this.email = email;
        this.reward = reward;
        this.version = version;
        this.ts = ts;
    }
}

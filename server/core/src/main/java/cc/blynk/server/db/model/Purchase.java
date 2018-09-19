package cc.blynk.server.db.model;

import java.util.Date;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.03.16.
 */
public class Purchase {

    public final String email;

    public final int reward;

    public final String transactionId;

    public final double price;

    public Date date;

    public Purchase(String email, int reward, double price, String transactionId) {
        this.email = email;
        this.reward = reward;
        this.transactionId = transactionId;
        this.price = price;
    }
}

package cc.blynk.server.db.model;

import java.util.Date;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.03.16.
 */
public class Purchase {

    public String email;

    public int reward;

    public String transactionId;

    public double price;

    public Date date;

    public Purchase(String email, int reward, String transactionId) {
        this.email = email;
        this.reward = reward;
        this.transactionId = transactionId;
        this.price = calcPrice(reward);
    }

    private static double calcPrice(int reward) {
        switch (reward) {
            case 200 :
                return 0;
            case 1000 :
                return 0.99;
            case 2400 :
                return 1.99;
            case 5000 :
                return 3.99;
            case 13000 :
                return 9.99;
            case 28000 :
                return 19.99;
            default:
                return -1;
        }
    }
}

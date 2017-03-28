package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.protocol.exceptions.EnergyLimitException;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ParseUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.ConcurrentMap;

/**
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 4:03 PM
 */
public class User {

    private static final int INITIAL_ENERGY_AMOUNT = ParseUtil.parseInt(System.getProperty("initial.energy", "2000"));

    //todo remove after migration
    public ConcurrentMap<Integer, String> dashTokens;

    public String name;

    //key fields
	public String email;
    public String appName;
    public String region;

    public volatile String pass;

    //used mostly to understand if user profile was changed, all other fields update ignored as it is not so important
    public volatile long lastModifiedTs;

    public String lastLoggedIP;
    public long lastLoggedAt;

    public Profile profile;

    public boolean isFacebookUser;
    public boolean isSuperAdmin;

    public volatile int energy;

    public transient int emailMessages;
    public transient long emailSentTs;

    public User() {
        this.lastModifiedTs = System.currentTimeMillis();
        this.profile = new Profile();
        this.energy = INITIAL_ENERGY_AMOUNT;
        this.isFacebookUser = false;
        this.appName = AppName.BLYNK;
    }

    public User(String email, String pass, String appName, String region, boolean isFacebookUser, boolean isSuperAdmin) {
        this();
        this.email = email;
        this.name = email;
        this.pass = pass;
        this.appName = appName;
        this.region = region;
        this.isFacebookUser = isFacebookUser;
        this.isSuperAdmin = isSuperAdmin;
    }

    @JsonProperty("id")
    private String id() {
        return email + "-" + appName;
    }

    public void subtractEnergy(int price) {
        if (AppName.BLYNK.equals(appName) && price > energy) {
            throw new EnergyLimitException("Not enough energy.");
        }
        //non-atomic. we are fine with that
        this.energy -= price;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    public void recycleEnergy(int price) {
        purchaseEnergy(price);
    }

    public void purchaseEnergy(int price) {
        //non-atomic. we are fine with that
        this.energy += price;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;

        User user = (User) o;

        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        return !(appName != null ? !appName.equals(user.appName) : user.appName != null);

    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

}

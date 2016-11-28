package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.protocol.exceptions.EnergyLimitException;
import cc.blynk.utils.JsonParser;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 4:03 PM
 */
public class User {

    private static final int INITIAL_ENERGY_AMOUNT = 2000;

    public ConcurrentMap<Integer, String> dashShareTokens;

    //todo remove after migration
    public ConcurrentMap<Integer, String> dashTokens;

    //key fields
	public String name;
    public String appName;
    public String region;

    public volatile String pass;

    //used mostly to understand if user profile was changed, all other fields update ignored as it is not so important
    public volatile long lastModifiedTs;

    public long lastLoggedAt;

    public Profile profile;

    public boolean isFacebookUser;

    private volatile int energy;

    public transient int emailMessages;
    public transient long emailSentTs;

    public User() {
        this.lastModifiedTs = System.currentTimeMillis();
        this.profile = new Profile();
        this.dashShareTokens = new ConcurrentHashMap<>();
        this.dashTokens = new ConcurrentHashMap<>();
        this.energy = INITIAL_ENERGY_AMOUNT;
        this.isFacebookUser = false;
        this.appName = AppName.BLYNK;
    }

    public User(String name, String pass, String appName, String region, boolean isFacebookUser) {
        this();
        this.name = name;
        this.pass = pass;
        this.appName = appName;
        this.region = region;
        this.isFacebookUser = isFacebookUser;
    }

    @JsonProperty("id")
    private String id() {
        return name + "-" + appName;
    }

    public boolean dashIdExists(int dashId) {
        for (DashBoard dashBoard : profile.dashBoards) {
            if (dashBoard.id == dashId) {
                return true;
            }
        }

        return false;
    }

    private void checkPrice(int price) {
        if (AppName.BLYNK.equals(appName) && price > energy) {
            throw new EnergyLimitException("Not enough energy.");
        }
    }

    public void subtractEnergy(int price) {
        checkPrice(price);
        this.energy -= price;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    public void recycleEnergy(int price) {
        this.energy += price;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    public void purchaseEnergy(int price) {
        this.energy += price;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    public int getEnergy() {
        return energy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

		return !(name != null ? !name.equals(user.name) : user.name != null);

	}

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

}

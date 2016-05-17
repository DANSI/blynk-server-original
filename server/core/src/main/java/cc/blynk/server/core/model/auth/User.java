package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.protocol.exceptions.EnergyLimitException;
import cc.blynk.utils.JsonParser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 4:03 PM
 */
public class User {

    public Map<Integer, String> dashShareTokens;
    public Map<Integer, String> dashTokens;

	public String name;

    public volatile String pass;

    //used mostly to understand if user profile was changed, all other fields update ignored as it is not so important
    public volatile long lastModifiedTs;

    public long lastLoggedAt;

    public Profile profile;
    public boolean isFacebookUser;
    public String appName;
    private int energy;

    public User() {
        this.lastModifiedTs = System.currentTimeMillis();
        this.profile = new Profile();
        this.dashShareTokens = new HashMap<>();
        this.dashTokens = new HashMap<>();
        this.energy = 2000;
        this.isFacebookUser = false;
    }

    public User(String name, String pass, String appName) {
        this();
        this.name = name;
        this.pass = pass;
        this.appName = appName;
    }

    //used for facebook login. available only for Blynk app.
    public User(String name, String appName) {
        this(name, null, appName);
        this.isFacebookUser = true;
    }

    public void setProfile(Profile profile) {
        //todo remove later.
        for (DashBoard dashBoard : profile.dashBoards) {
            for (DashBoard curDash : this.profile.dashBoards) {
                if (dashBoard.id == curDash.id) {
                    dashBoard.isActive = curDash.isActive;
                    break;
                }
            }
        }

        this.profile = profile;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    public boolean hasActive() {
        if (profile != null) {
            for (DashBoard dashBoard : profile.dashBoards) {
                if (dashBoard.isActive) {
                    return true;
                }
            }
        }
        return false;
    }

    public void putToken(Integer dashId, String token, Map<Integer, String> tokens) {
        cleanTokensForNonExistentDashes(tokens);
        tokens.put(dashId, token);
        this.lastModifiedTs = System.currentTimeMillis();
    }

    private void cleanTokensForNonExistentDashes(Map<Integer, String> tokens) {
        Iterator<Integer> iterator = tokens.keySet().iterator();
        while (iterator.hasNext()) {
            if (!exists(iterator.next())) {
                iterator.remove();
            }
        }
    }

    private boolean exists(int dashId) {
        for (DashBoard dashBoard : profile.dashBoards) {
            if (dashBoard.id == dashId) {
                return true;
            }
        }

        return false;
    }

    public Integer getDashIdByToken(String token) {
        for (Map.Entry<Integer, String> entry : dashTokens.entrySet()) {
            if (entry.getValue().equals(token)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }

    private void checkPrice(int price, int msgId) {
        if (AppName.BLYNK.equals(appName) && price > energy) {
            throw new EnergyLimitException("Not enough energy.", msgId);
        }
    }

    public void subtractEnergy(int price, int msgId) {
        checkPrice(price, msgId);
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

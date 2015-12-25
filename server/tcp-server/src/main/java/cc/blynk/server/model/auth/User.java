package cc.blynk.server.model.auth;

import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.Profile;
import cc.blynk.server.utils.JsonParser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 4:03 PM
 */
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

    public final Map<Integer, String> dashShareTokens;
    public final Map<Integer, String> dashTokens;

	public String name;

    public volatile String pass;

    //used mostly to understand if user profile was changed, all other fields update ignored as it is not so important
    public volatile long lastModifiedTs;

    public long lastLoggedAt;

    public Profile profile;

    public User() {
        this.lastModifiedTs = System.currentTimeMillis();
        this.profile = new Profile();
        this.dashShareTokens = new HashMap<>();
        this.dashTokens = new HashMap<>();
    }

    public User(String name, String pass) {
        this();
        this.name = name;
        this.pass = pass;
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

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

	private String name;
    private String pass;
    private String id;
    //used mostly to understand if user profile was changed, all other fields update ignored as it is not so important
    private long lastModifiedTs;
    //todo avoid volatile
    private volatile Profile profile;
    private Map<Integer, String> dashTokens = new HashMap<>();
    private Map<Integer, String> dashShareTokens = new HashMap<>();

    public User() {
        this.lastModifiedTs = System.currentTimeMillis();
        this.profile = new Profile();
    }

    public User(String name, String pass) {
        this();
        this.name = name;
        this.pass = pass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        //do not accept activeDashId from user, it is calculated field.
        profile.activeDashId = this.profile.activeDashId;
        this.profile = profile;
        this.lastModifiedTs = System.currentTimeMillis();
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
        if (profile.getDashBoards() != null) {
            for (DashBoard dashBoard : profile.getDashBoards()) {
                if (dashBoard.getId() == dashId) {
                    return true;
                }
            }
        }

        return false;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<Integer, String> getDashTokens() {
        return dashTokens;
    }

    public void setDashTokens(Map<Integer, String> dashTokens) {
        this.dashTokens = dashTokens;
    }

    public long getLastModifiedTs() {
        return lastModifiedTs;
    }

    public void setLastModifiedTs(long lastModifiedTs) {
        this.lastModifiedTs = lastModifiedTs;
    }

    public Map<Integer, String> getDashShareTokens() {
        return dashShareTokens;
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

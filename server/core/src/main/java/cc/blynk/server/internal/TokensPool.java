package cc.blynk.server.internal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
public final class TokensPool {

    private static final Logger log = LogManager.getLogger(TokensPool.class);

    private final long tokenExpirationPeriodMillis;
    private final ConcurrentMap<String, TokenUser> holder;

    public TokensPool(long expirationPeriodMillis) {
        this.holder = new ConcurrentHashMap<>();
        this.tokenExpirationPeriodMillis = expirationPeriodMillis;
    }

    public void addToken(String token, TokenUser user) {
        log.info("Adding token for {} user to the pool", user.email);
        cleanupOldTokens();
        holder.put(token, user);
    }

    public TokenUser getUser(String token) {
        cleanupOldTokens();
        return holder.get(token);
    }

    public boolean hasToken(String email, String appName) {
        for (Map.Entry<String, TokenUser> entry : holder.entrySet()) {
            TokenUser tokenUser = entry.getValue();
            if (tokenUser.isSame(email, appName)) {
                return true;
            }
        }
        return false;
    }

    public void removeToken(String token) {
        holder.remove(token);
    }

    public int size() {
        return holder.size();
    }

    public void cleanupOldTokens() {
        long now = System.currentTimeMillis();
        holder.entrySet().removeIf(entry -> entry.getValue().createdAt + tokenExpirationPeriodMillis < now);
    }

    //just for tests
    public ConcurrentMap<String, TokenUser> getHolder() {
        return holder;
    }
}

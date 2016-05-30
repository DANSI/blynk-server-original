package cc.blynk.server.api.http.pojo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
public final class TokensPool {

    private static final Logger log = LogManager.getLogger(TokensPool.class);
    private static final int TOKEN_EXPIRATION_DEFAULT_PERIOD_IN_MIN = 60;
    private final ConcurrentMap<String, TokenUser> holder;

    public TokensPool() {
        Cache<String, TokenUser> cache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .expireAfterWrite(TOKEN_EXPIRATION_DEFAULT_PERIOD_IN_MIN, TimeUnit.MINUTES)
                .build();

        this.holder = cache.asMap();
    }


    public void addToken(String token, TokenUser user) {
        log.info("Adding token for {} user to the pool", user.email);
        holder.put(token, user);
    }

    public TokenUser getUser(String token) {
        return holder.get(token);
    }

    public void removeToken(String token) {
        holder.remove(token);
    }

    public int size() {
        return holder.size();
    }

}

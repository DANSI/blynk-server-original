package cc.blynk.server.internal;

import cc.blynk.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.internal.SerializationUtil.deserialize;
import static cc.blynk.server.internal.SerializationUtil.serialize;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
public final class TokensPool implements Closeable {

    private static final Logger log = LogManager.getLogger(TokensPool.class);
    private static final String TOKENS_TEMP_FILENAME = "tokens_pool_temp.bin";

    private final long tokenExpirationPeriodMillis;
    private final String dataFolder;
    private final ConcurrentHashMap<String, TokenUser> tokens;

    @SuppressWarnings("unchecked")
    public TokensPool(String dataFolder, long expirationPeriodMillis) {
        this.tokenExpirationPeriodMillis = expirationPeriodMillis;
        this.dataFolder = dataFolder;

        Path path = Paths.get(dataFolder, TOKENS_TEMP_FILENAME);
        this.tokens = (ConcurrentHashMap<String, TokenUser>) deserialize(path);
        FileUtils.deleteQuietly(path);
    }

    public void addToken(String token, TokenUser user) {
        log.info("Adding token for {} user to the pool", user.email);
        cleanupOldTokens();
        tokens.put(token, user);
    }

    public TokenUser getUser(String token) {
        cleanupOldTokens();
        return tokens.get(token);
    }

    public boolean hasToken(String email, String appName) {
        for (Map.Entry<String, TokenUser> entry : tokens.entrySet()) {
            TokenUser tokenUser = entry.getValue();
            if (tokenUser.isSame(email, appName)) {
                return true;
            }
        }
        return false;
    }

    public void removeToken(String token) {
        tokens.remove(token);
    }

    public int size() {
        return tokens.size();
    }

    public void cleanupOldTokens() {
        long now = System.currentTimeMillis();
        tokens.entrySet().removeIf(entry -> entry.getValue().createdAt + tokenExpirationPeriodMillis < now);
    }

    //just for tests
    public ConcurrentHashMap<String, TokenUser> getTokens() {
        return tokens;
    }

    @Override
    public void close() {
        serialize(Paths.get(dataFolder, TOKENS_TEMP_FILENAME), tokens);
    }
}

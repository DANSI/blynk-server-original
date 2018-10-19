package cc.blynk.server.internal.token;

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
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.10.18.
 */
public final class TokensPool implements Closeable {

    private static final Logger log = LogManager.getLogger(TokensPool.class);
    private static final String TOKENS_TEMP_FILENAME = "tokens_pool_temp.bin";

    private final String dataFolder;
    private final ConcurrentHashMap<String, BaseToken> tokens;

    @SuppressWarnings("unchecked")
    public TokensPool(String dataFolder) {
        this.dataFolder = dataFolder;

        Path path = Paths.get(dataFolder, TOKENS_TEMP_FILENAME);
        this.tokens = (ConcurrentHashMap<String, BaseToken>) deserialize(path);
        FileUtils.deleteQuietly(path);
    }

    public void addToken(String token, ResetPassToken user) {
        log.info("Adding token for {} user to the pool", user.email);
        cleanupOldTokens();
        tokens.put(token, user);
    }

    public ResetPassToken getResetPassToken(String token) {
        BaseToken baseToken = getBaseToken(token);
        if (baseToken instanceof ResetPassToken) {
            return (ResetPassToken) baseToken;
        }
        return null;
    }

    public BaseToken getBaseToken(String token) {
        cleanupOldTokens();
        return tokens.get(token);
    }

    public boolean hasResetToken(String email, String appName) {
        for (Map.Entry<String, BaseToken> entry : tokens.entrySet()) {
            BaseToken tokenBase = entry.getValue();
            if (tokenBase instanceof ResetPassToken) {
                ResetPassToken resetPassToken = (ResetPassToken) tokenBase;
                if (resetPassToken.isSame(email, appName)) {
                    return true;
                }
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
        tokens.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    //just for tests
    public ConcurrentHashMap<String, BaseToken> getTokens() {
        return tokens;
    }

    @Override
    public void close() {
        serialize(Paths.get(dataFolder, TOKENS_TEMP_FILENAME), tokens);
    }
}

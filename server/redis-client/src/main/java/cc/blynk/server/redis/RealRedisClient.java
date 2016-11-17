package cc.blynk.server.redis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.10.16.
 */
public class RealRedisClient implements RedisClient {

    public static final String REDIS_PROPERTIES = "redis.properties";
    public static final int USER_DB_INDEX = 0;
    public static final int TOKEN_DB_INDEX = 1;

    private static final Logger log = LogManager.getLogger(RealRedisClient.class);

    private final JedisPool tokenPool;
    private final JedisPool userPool;

    public RealRedisClient(Properties props) {
        this(props.getProperty("redis.host"), props.getProperty("redis.pass"), props.getProperty("redis.port"));
    }

    protected RealRedisClient(String host, String pass, String port) {
        this(host, pass, (port == null ? Protocol.DEFAULT_PORT : Integer.parseInt(port)));
    }

    protected RealRedisClient(String host, String pass, int port) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);
        this.userPool = new JedisPool(config, host, port, Protocol.DEFAULT_TIMEOUT, pass, USER_DB_INDEX);
        this.tokenPool = new JedisPool(config, host, port, Protocol.DEFAULT_TIMEOUT, pass, TOKEN_DB_INDEX);
        checkConnected();
        log.info("Redis pool successfully initialized on {}:{}", host, port);
    }

    private void checkConnected() {
        try (Jedis jedis = userPool.getResource()) {
        }
    }

    @Override
    public String getServerByUser(String user) {
        try (Jedis jedis = userPool.getResource()) {
            return jedis.get(user);
        } catch (Exception e) {
            log.error("Error getting server by user {}", user, e);
        }
        return null;
    }

    @Override
    public String getServerByToken(String token) {
        try (Jedis jedis = tokenPool.getResource()) {
            return jedis.get(token);
        } catch (Exception e) {
            log.error("Error getting server by token {}.", token, e);
        }
        return null;
    }

    @Override
    public void assignServerToToken(String token, String server) {
        try (Jedis jedis = tokenPool.getResource()) {
            jedis.set(token, server);
        } catch (Exception e) {
            log.error("Error setting server {} to token {}.", server, token, e);
        }
    }

    @Override
    public void assignServerToUser(String username, String server) {
        try (Jedis jedis = userPool.getResource()) {
            jedis.set(username, server);
        } catch (Exception e) {
            log.error("Error setting server {} to user {}.", server, username, e);
        }
    }

    @Override
    public void removeToken(String... tokens) {
        try (Jedis jedis = tokenPool.getResource()) {
            jedis.del(tokens);
        }
    }

    public JedisPool getTokenPool() {
        return tokenPool;
    }

    public JedisPool getUserPool() {
        return userPool;
    }

    @Override
    public void close() {
        System.out.println("Stopping Redis...");
        try {
            userPool.destroy();
        } catch (Exception e) {
        }
        try {
            tokenPool.destroy();
        } catch (Exception e) {
        }
    }
}

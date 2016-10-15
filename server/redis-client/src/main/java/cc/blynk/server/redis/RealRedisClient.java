package cc.blynk.server.redis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.10.16.
 */
public class RealRedisClient implements Closeable, RedisClient {

    public static final String REDIS_PROPERTIES = "redis.properties";

    private static final Logger log = LogManager.getLogger(RealRedisClient.class);

    private final JedisPool pool;

    public RealRedisClient(Properties props) {
        this(props.getProperty("redis.host"), props.getProperty("redis.pass"));
    }

    protected RealRedisClient(String host, String pass) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(20);
        config.setBlockWhenExhausted(true);
        this.pool = new JedisPool(config, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, pass, Protocol.DEFAULT_DATABASE);
        checkConnected();
    }

    private void checkConnected() {
        try (Jedis jedis = pool.getResource()) {
        }
    }

    @Override
    public String getServerByToken(String token) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(token);
        } catch (Exception e) {
            log.error("Error making request to redis.", e);
        }
        return null;
    }

    @Override
    public void setServerByToken(String token, String server) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(token, server);
        }
    }

    //only for tests
    @Override
    public Jedis getClient() {
        return pool.getResource();
    }

    @Override
    public void close() throws IOException {
        pool.destroy();
    }
}

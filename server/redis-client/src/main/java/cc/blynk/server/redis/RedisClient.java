package cc.blynk.server.redis;

import redis.clients.jedis.Jedis;

import java.io.Closeable;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.10.16.
 */
public interface RedisClient extends Closeable {

    String getServerByToken(String token);

    void assignServerToToken(String token, String server);

    void assignServerToUser(String username, String server);

    void removeToken(String token);

    Jedis getUserClient();

    Jedis getTokenClient();

    @Override
    void close();
}

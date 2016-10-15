package cc.blynk.server.redis;

import redis.clients.jedis.Jedis;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.10.16.
 */
public interface RedisClient {

    String getServerByToken(String token);

    void setServerByToken(String token, String server);

    //only for tests
    Jedis getClient();
}

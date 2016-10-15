package cc.blynk.server.redis;

import redis.clients.jedis.Jedis;

/**
 * No need in redis on local servers.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.10.16.
 */
public class FakeRedisClient implements RedisClient {

    @Override
    public String getServerByToken(String token) {
        return null;
    }

    @Override
    public void setServerByToken(String token, String server) {

    }

    @Override
    public Jedis getClient() {
        return null;
    }
}

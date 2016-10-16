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
    public void assignServerToToken(String token, String server) {
    }

    @Override
    public void assignServerToUser(String username, String server) {
    }

    @Override
    public Jedis getUserClient() {
        return null;
    }

    @Override
    public void removeToken(String token) {

    }

    @Override
    public Jedis getTokenClient() {
        return null;
    }
}

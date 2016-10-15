package cc.blynk.server.redis;

import org.junit.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.10.16.
 */
public class RedisClientTest {

    @Test(expected = JedisConnectionException.class)
    public void testCreationFailed() {
        RealRedisClient redisClient = new RealRedisClient("localhost", "");
        assertEquals(null, redisClient);
    }

    @Test
    public void testGetTestString() {
        RealRedisClient redisClient = new RealRedisClient("localhost", "5b56cc09a8e7305b9761706350d277eb7b9a3815cf5fe6932a04ea82170a2917");
        String result = redisClient.getServerByToken("test");
        assertEquals("It's working!", result);
    }


}

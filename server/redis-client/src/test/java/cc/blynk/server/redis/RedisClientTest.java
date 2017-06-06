package cc.blynk.server.redis;

import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.10.16.
 */
public class RedisClientTest {

    @Test(expected = JedisConnectionException.class)
    public void testCreationFailed() {
        RedisClient redisClient = new RedisClient("localhost", "", 6378, true);
        assertNull(redisClient);
    }

    @Test
    @Ignore
    public void testGetTestString() {
        RedisClient redisClient = new RedisClient("localhost", "pass123", 6378, true);
        String result = redisClient.getServerByToken("test");
        assertEquals("It's working!", result);
    }


}

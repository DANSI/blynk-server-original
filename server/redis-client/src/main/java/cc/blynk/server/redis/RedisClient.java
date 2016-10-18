package cc.blynk.server.redis;

import java.io.Closeable;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.10.16.
 */
public interface RedisClient extends Closeable {

    String getServerByToken(String token);

    String getServerByUser(String user);

    void assignServerToToken(String token, String server);

    void assignServerToUser(String username, String server);

    void removeToken(String token);

    @Override
    void close();
}

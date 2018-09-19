package cc.blynk.server.internal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.09.18.
 */
public final class SerializationUtil {

    private final static Logger log = LogManager.getLogger(SerializationUtil.class);

    private SerializationUtil() {
    }

    public static Object deserialize(Path path) {
        if (Files.exists(path)) {
            try {
                return deserializeObject(path);
            } catch (Exception e) {
                log.error(e);
            }
        }

        return new ConcurrentHashMap<>();
    }

    public static void serialize(Path path, Map<?, ?> map) {
        if (map.size() > 0) {
            try {
                serializeObject(path, map);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private static Object deserializeObject(Path path) throws IOException, ClassNotFoundException {
        try (InputStream is = Files.newInputStream(path);
             ObjectInputStream objectinputstream = new ObjectInputStream(is)) {
            return objectinputstream.readObject();
        }
    }

    private static void serializeObject(Path path, Object obj) throws IOException {
        try (OutputStream os = Files.newOutputStream(path);
             ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(obj);
        }
    }

}

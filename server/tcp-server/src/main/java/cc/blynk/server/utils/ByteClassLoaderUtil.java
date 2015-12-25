package cc.blynk.server.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class used for creating class instance from byte array.
 * In other - words admin can execute custom code on server side.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ByteClassLoaderUtil extends ClassLoader {

    public static byte[] readClassBytesFromAsResource(Class<?> clazz, String className) throws IOException {
        try (InputStream is = clazz.getResourceAsStream(className)) {
            //max 100 kb
            byte[] bytes = new byte[100 * 1024];
            int length = IOUtils.read(is, bytes);

            byte[] res = new byte[length];
            System.arraycopy(bytes, 0, res, 0, length);
            return res;
        }
    }

}

package cc.blynk.server.core.administration;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ByteClassLoader extends ClassLoader {

    public Executable defineClass(byte[] classData) throws Exception {
        return defineClass(classData, classData.length);
    }

    public Executable defineClass(byte[] classData, int length) throws Exception {
        Class<?> clazz = defineClass(null, classData, 0, length);
        return (Executable) clazz.newInstance();
    }

}

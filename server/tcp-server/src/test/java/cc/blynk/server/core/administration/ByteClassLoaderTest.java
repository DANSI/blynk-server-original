package cc.blynk.server.core.administration;

import cc.blynk.server.utils.ByteClassLoaderUtil;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ByteClassLoaderTest {

    @Test
    public void correctClassLoadFromBytes() throws Exception {
        ByteClassLoaderUtil byteClassLoaderUtil = new ByteClassLoaderUtil();

        byte[] classBytes = ByteClassLoaderUtil.readClassBytesFromAsResource(this.getClass(), "ExecutorImplementation.class");


        Executable executable = byteClassLoaderUtil.defineClass(classBytes);
        List<String> res = executable.execute(null, null);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("Test success!", res.get(0));
    }

}

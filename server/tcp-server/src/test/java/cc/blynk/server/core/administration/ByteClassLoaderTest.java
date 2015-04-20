package cc.blynk.server.core.administration;

import cc.blynk.server.utils.ByteClassLoaderUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ByteClassLoaderTest {

    @Test
    public void correctClassLoadFromBytes() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("ExecutorImplementation.class")) {
            //100 kb
            byte[] bytes = new byte[100 * 1024 * 1024];
            int length = IOUtils.read(is, bytes);

            ByteClassLoaderUtil byteClassLoaderUtil = new ByteClassLoaderUtil();
            Executable executable = byteClassLoaderUtil.defineClass(bytes, length);
            String res = executable.execute(null, null);

            assertEquals("Test success!", res);
        }
    }

}

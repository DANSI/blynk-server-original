package cc.blynk.utils.structure;

import cc.blynk.utils.TokenGeneratorUtil;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.01.18.
 */
public class TokenGeneratorUtilTest {

    @Test
    public void testCorrectWork() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid.toString().replace("-", ""), TokenGeneratorUtil.fastToString(uuid));
    }

}

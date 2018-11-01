package cc.blynk.server.core.model;

import cc.blynk.server.core.model.serialization.CopyUtil;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

public class CopyObjectTest {

    @Test
    public void testDeepCopy() {
        Profile profile = new Profile();

        Profile copy = CopyUtil.deepCopy(profile);
        assertNotNull(copy);
        assertNotSame(copy, profile);
        assertNotSame(copy.pinsStorage, profile.pinsStorage);
    }

}

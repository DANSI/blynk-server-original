package cc.blynk.server.core.model;

import cc.blynk.server.core.model.serialization.CopyUtil;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

public class CopyObjectTest {

    @Test
    public void testDeepCopy() {
        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 1;
        dashBoard.name = "123";
        dashBoard.pinsStorage = new HashMap<>();

        DashBoard copy = CopyUtil.deepCopy(dashBoard);
        assertNotNull(copy);
        assertNotSame(copy, dashBoard);
        assertNotSame(copy.pinsStorage, dashBoard.pinsStorage);
        assertEquals("123", copy.name);
        assertEquals(1, copy.id);
    }

}

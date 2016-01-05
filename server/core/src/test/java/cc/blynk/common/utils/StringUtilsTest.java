package cc.blynk.common.utils;

import cc.blynk.utils.StringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class StringUtilsTest {

    @Test
    public void testCorrectFastNewSplit() {
        String in = "ar 1 2 3 4 5 6".replaceAll(" ", "\0");

        String res = StringUtils.fetchPin(in);
        assertNotNull(res);
        assertEquals("1", res);


        in = "ar 22222".replaceAll(" ", "\0");
        res = StringUtils.fetchPin(in);
        assertNotNull(res);
        assertEquals("22222", res);

        in = "1 1".replaceAll(" ", "\0");
        res = StringUtils.fetchPin(in);
        assertNotNull(res);
        assertEquals("", res);
    }

}

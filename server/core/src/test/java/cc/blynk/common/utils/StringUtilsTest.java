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

    @Test
    public void testCorrectSplit3() {
        String[] res = StringUtils.split3("aw 1 2".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        assertEquals("aw", res[0]);
        assertEquals("1", res[1]);
        assertEquals("2", res[2]);

        res = StringUtils.split3("dw 11 22".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));
        assertEquals("dw", res[0]);
        assertEquals("11", res[1]);
        assertEquals("22", res[2]);

        String s = "sdfsafdfdgfjdasfjds;lfjd;lsf dsfld;las fd;slaj fd;lsfj das";
        res = StringUtils.split3("vw 255 ".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING) + s);
        assertEquals("vw", res[0]);
        assertEquals("255", res[1]);
        assertEquals(s, res[2]);

        s = "sdfsafdfdgfjdasfjds;lfjd;lsf\0dsfld;las\0fd;slaj\0fd;lsfj\0das";
        res = StringUtils.split3("vw 255 ".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING) + s);
        assertEquals("vw", res[0]);
        assertEquals("255", res[1]);
        assertEquals(s, res[2]);
    }

}

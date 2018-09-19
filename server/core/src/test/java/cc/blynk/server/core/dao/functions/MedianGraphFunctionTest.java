package cc.blynk.server.core.dao.functions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.07.17.
 */
public class MedianGraphFunctionTest {

    @Test
    public void testMedianFunction() {
        MedianGraphFunction medianFunction = new MedianGraphFunction();
        medianFunction.apply(0);
        assertEquals(0, medianFunction.getResult(), 0.0001);

        medianFunction.apply(1);
        assertEquals(0.5, medianFunction.getResult(), 0.0001);

        medianFunction.apply(2);
        assertEquals(1, medianFunction.getResult(), 0.0001);

        medianFunction.apply(3);
        assertEquals(1.5, medianFunction.getResult(), 0.0001);
    }

    @Test
    public void testMedianFunction2() {
        MedianGraphFunction medianFunction = new MedianGraphFunction();
        medianFunction.apply(0);
        medianFunction.apply(0);
        medianFunction.apply(0);
        medianFunction.apply(0);
        medianFunction.apply(0);
        medianFunction.apply(0);
        assertEquals(0, medianFunction.getResult(), 0.0001);
    }

}

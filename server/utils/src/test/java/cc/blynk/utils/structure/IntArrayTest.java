package cc.blynk.utils.structure;

import cc.blynk.utils.IntArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntArrayTest {

    @Test
    public void test() {
        IntArray intArray = new IntArray();
        assertEquals(0, intArray.toArray().length);

        intArray.add(10);
        int[] result = intArray.toArray();
        assertEquals(1, result.length);
        assertEquals(10, result[0]);

        intArray = new IntArray();
        for (int i = 0; i < 1000; i++) {
            intArray.add(i);
        }

        result = intArray.toArray();
        assertEquals(1000, result.length);

        for (int i = 0; i < 1000; i++) {
            assertEquals(i, result[i]);
        }

    }

}

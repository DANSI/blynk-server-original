package cc.blynk.utils;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.05.16.
 */
public class LimitedQueueTest {

    @Test
    public void test() {
        List<String> list = new LimitedQueue<>(2);
        list.add("1");
        list.add("2");
        list.add("3");
        assertEquals(2, list.size());
        assertEquals("2", list.get(0));
        assertEquals("3", list.get(1));
    }

}

package cc.blynk.utils.structure;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.05.16.
 */
public class LimitedQueueTest {

    @Test
    public void addLimitTest() {
        BaseLimitedQueue<String> list = new BaseLimitedQueue<>(2);
        list.add("1");
        list.add("2");
        list.add("3");
        assertEquals(2, list.size());
        assertEquals("2", list.poll());
        assertEquals("3", list.poll());
    }

    private static BaseLimitedQueue<String> makeList() {
        return new BaseLimitedQueue<>(3) {{
            add("1");
            add("2");
            add("3");
        }
        };
    }

}

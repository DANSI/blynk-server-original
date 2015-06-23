package cc.blynk.server.notifications.push;

import org.junit.Ignore;
import org.junit.Test;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/8/2015.
 */
public class GCMWrapperTest {

    @Test
    @Ignore
    public void testSendMessage() throws Exception {
        new GCMWrapper().send("534534534534", "Hello World");
        Thread.sleep(1000);
    }

}

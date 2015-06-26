package cc.blynk.server.notifications;

import org.junit.Ignore;
import org.junit.Test;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMWrapperTest {

    @Test
    @Ignore
    public void test() throws Exception {
        GCMWrapper gcmWrapper = new GCMWrapper();
        gcmWrapper.send("YOUR_TOKEN", "yo");
    }

}

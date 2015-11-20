package cc.blynk.server.notifications;

import cc.blynk.server.model.widgets.notifications.Priority;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMWrapperTest {

    @Test
    @Ignore
    public void testIOS() throws Exception {
        GCMWrapper gcmWrapper = new GCMWrapper();
        gcmWrapper.send(new IOSGCMMessage("", "yo!!!", 1));
    }

    @Test
    @Ignore
    public void testAndroid() throws Exception {
        GCMWrapper gcmWrapper = new GCMWrapper();
        gcmWrapper.send(new AndroidGCMMessage("", Priority.normal, "yo!!!", 1));
    }

    @Test
    public void testJson() {
        assertEquals("{\"to\":\"to\",\"priority\":\"normal\",\"data\":{\"message\":\"yo!!!\",\"dashId\":1}}", new AndroidGCMMessage("to", Priority.normal, "yo!!!", 1).toString());
    }
}

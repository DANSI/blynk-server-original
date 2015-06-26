package cc.blynk.server.notifications;

import org.junit.Test;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMWrapperTest {

    @Test
    public void test() throws Exception {
        GCMWrapper gcmWrapper = new GCMWrapper();
        gcmWrapper.send("d6eWuYmrc6A:APA91bGCH5161vtWlD-BhZ2XrVB58GOe4s4IyMg6eEMSiMi7hdP9PMAXD0xS2JVW-d3EMQvL6gFClWpcvZXTxVjMFMCD293rg4iAPSIfGMNlaL0_uHq9IxpAoHmhRjYTGVkK7tjo4YTO", "yo");
    }

}

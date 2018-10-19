package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.utils.AppNameUtil;
import org.junit.Test;

import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;

public class RegistrationLimitCheckTest extends SingleServerInstancePerTest {

    @Test
    public void registrationLimitCheck() throws Exception {
        for (int i = 0; i < 100; i++) {
            TestAppClient appClient = new TestAppClient(properties);
            appClient.start();
            appClient.register(incrementAndGetUserName(), "1", AppNameUtil.BLYNK);
            appClient.verifyResult(ok(1));
            appClient.stop();
        }

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.register(incrementAndGetUserName(), "1", AppNameUtil.BLYNK);
        appClient.verifyResult(notAllowed(1));
    }

}

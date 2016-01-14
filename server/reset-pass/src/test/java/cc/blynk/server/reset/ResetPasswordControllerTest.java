package cc.blynk.server.reset;

import cc.blynk.server.reset.web.controller.ResetPasswordController;
import cc.blynk.server.reset.web.entities.TokensPool;
import cc.blynk.utils.ServerProperties;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordControllerTest {

    @Test
    @Ignore
    public void testGetResetPasswordPage() throws Exception {
        ResetPasswordController controller = new ResetPasswordController("http://localhost", 8080, new TokensPool(), new ServerProperties());
        String resetPasswordPage = controller.getResetPasswordPage("", "");
        assertNotNull(resetPasswordPage);
    }
}

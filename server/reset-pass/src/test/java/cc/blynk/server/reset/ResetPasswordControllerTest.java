package cc.blynk.server.reset;

import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.reset.web.controller.ResetPasswordController;
import cc.blynk.server.reset.web.entities.TokensPool;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class ResetPasswordControllerTest {

    @Mock
    private MailWrapper mailWrapper;
    @Mock
    private Runnable runnable;

    @Test
    public void testGetResetPasswordPage() throws Exception {
        ResetPasswordController controller = new ResetPasswordController("http://localhost", 8080, new TokensPool(), mailWrapper);
        String resetPasswordPage = controller.getResetPasswordPage("", "");
        assertNotNull(resetPasswordPage);
    }
}

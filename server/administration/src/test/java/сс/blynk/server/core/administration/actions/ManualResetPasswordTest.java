package сс.blynk.server.core.administration.actions;

import cc.blynk.server.core.administration.actions.ManualResetPassword;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.model.auth.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.04.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManualResetPasswordTest {

    @Mock
    private UserRegistry userRegistry;

    @Mock
    private User user;

    @Test
    public void testPassChanged() {
        ManualResetPassword manualResetPassword = new ManualResetPassword();

        String username = "dima@dima.ua";
        String pass = "123";
        when(userRegistry.getByName(username)).thenReturn(user);
        List<String> response = manualResetPassword.execute(userRegistry, null, username, pass);

        assertEquals(user.pass, "UDgMLjFcZ/HDe1jFqejmJIGh8aOc1V7xSLVUKR5hmsk=");

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("Password updated.\n", response.get(0));
        assertEquals("ok\n", response.get(1));
    }

    @Test
    public void testNoUser() {
        ManualResetPassword manualResetPassword = new ManualResetPassword();

        String username = "dima@dima.ua";
        String pass = "123";
        when(userRegistry.getByName(username)).thenReturn(null);
        List<String> repsonse = manualResetPassword.execute(userRegistry, null, username, pass);

        assertNotNull(repsonse);
        assertEquals(1, repsonse.size());
        assertEquals("ok\n", repsonse.get(0));
    }


}

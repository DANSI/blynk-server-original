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
import static org.mockito.Mockito.*;

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
        List<String> repsonse = manualResetPassword.execute(userRegistry, null, username, pass);

        verify(user).setPass(eq("UDgMLjFcZ/HDe1jFqejmJIGh8aOc1V7xSLVUKR5hmsk="));
        verify(user).setLastModifiedTs(any(long.class));

        assertNotNull(repsonse);
        assertEquals(2, repsonse.size());
        assertEquals("Password updated.\n", repsonse.get(0));
        assertEquals("ok\n", repsonse.get(1));
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

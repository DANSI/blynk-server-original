package сс.blynk.server.administration;

import cc.blynk.server.administration.actions.ResetPassword;
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
public class ResetPasswordTest {

    @Mock
    private UserRegistry userRegistry;

    @Mock
    private User user;

    @Test
    public void testPassChanged() {
        ResetPassword resetPassword = new ResetPassword();

        String username = "dima@dima.ua";
        String pass = "123";
        when(userRegistry.getByName(username)).thenReturn(user);
        List<String> repsonse = resetPassword.execute(userRegistry, null, username, pass);

        verify(user).setPass(eq("UDgMLjFcZ/HDe1jFqejmJIGh8aOc1V7xSLVUKR5hmsk="));
        verify(user).setLastModifiedTs(any(long.class));

        assertNotNull(repsonse);
        assertEquals(2, repsonse.size());
        assertEquals("Password updated.\n", repsonse.get(0));
        assertEquals("ok\n", repsonse.get(1));
    }

    @Test
    public void testNoUser() {
        ResetPassword resetPassword = new ResetPassword();

        String username = "dima@dima.ua";
        String pass = "123";
        when(userRegistry.getByName(username)).thenReturn(null);
        List<String> repsonse = resetPassword.execute(userRegistry, null, username, pass);

        assertNotNull(repsonse);
        assertEquals(1, repsonse.size());
        assertEquals("ok\n", repsonse.get(0));
    }


}

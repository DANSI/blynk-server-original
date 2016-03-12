package cc.blynk.server.reset;

import cc.blynk.server.admin.http.pojo.TokenUser;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserTest {

    @Test
    public void testUserEmail() {
        final String email = "test@gmail.com";
        final TokenUser user = new TokenUser(email);
        assertEquals(email, user.getEmail());
    }

    @Test
    public void testUserPassword(){
        final TokenUser user = new TokenUser("test@gmail.com");
        assertEquals("", user.getNewPassword());
        final String password = "password";
        user.setNewPassword(password);
        assertEquals(password, user.getNewPassword());
    }

    @Test
    public void testUserToken() {
        final TokenUser user = new TokenUser("test@gmail.com");
        assertEquals("", user.getNewPassword());
        final long resetPasswordTokenTs = 123L;
        user.setResetPasswordTokenTs(resetPasswordTokenTs);
        assertEquals(resetPasswordTokenTs, user.getResetPasswordTokenTs());
    }
}

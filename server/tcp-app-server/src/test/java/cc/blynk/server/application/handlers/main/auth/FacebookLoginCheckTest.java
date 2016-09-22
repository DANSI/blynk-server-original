package cc.blynk.server.application.handlers.main.auth;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.04.16.
 */
public class FacebookLoginCheckTest {

    @Test(expected = IOException.class)
    @Ignore("token can expire here, so ignore")
    public void testInvalidToken() throws Exception {
        FacebookLoginCheck facebookLoginCheck = new FacebookLoginCheck(null);
        facebookLoginCheck.verify("username", "token");
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore("token can expire here, so ignore")
    public void testInvalidEmailForToken() throws Exception {
        FacebookLoginCheck facebookLoginCheck = new FacebookLoginCheck(null);
        facebookLoginCheck.verify("username", "");
    }

    @Test
    @Ignore("token can expire here, so ignore")
    public void testValidEmailForToken() throws Exception {
        FacebookLoginCheck facebookLoginCheck = new FacebookLoginCheck(null);
        facebookLoginCheck.verify("valid email", "");
    }

}

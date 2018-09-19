package cc.blynk.server.reset;

import cc.blynk.server.internal.TokenUser;
import cc.blynk.server.internal.TokensPool;
import cc.blynk.utils.AppNameUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class TokensPoolTest {

    private static final int expirationPeriod = 60 * 60 * 1000;

    @Test
    public void addTokenTest() {
        final TokenUser user = new TokenUser("test.gmail.com", AppNameUtil.BLYNK);
        final String token = "123";
        final TokensPool tokensPool = new TokensPool(expirationPeriod);
        tokensPool.addToken(token, user);
        assertEquals(user, tokensPool.getUser(token));
    }

    @Test
    public void addTokenTwiceTest() {
        final TokenUser user = new TokenUser("test.gmail.com", AppNameUtil.BLYNK);
        final String token = "123";
        final TokensPool tokensPool = new TokensPool(expirationPeriod);
        tokensPool.addToken(token, user);
        tokensPool.addToken(token, user);
        assertEquals(1, tokensPool.size());
    }

    @Test
    public void remoteTokenTest() {
        final TokenUser user = new TokenUser("test.gmail.com", AppNameUtil.BLYNK);
        final String token = "123";
        final TokensPool tokensPool = new TokensPool(expirationPeriod);
        tokensPool.addToken(token, user);
        assertEquals(user, tokensPool.getUser(token));
        tokensPool.removeToken(token);
        assertEquals(0, tokensPool.size());
        assertNull(tokensPool.getUser(token));
    }
}

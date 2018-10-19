package cc.blynk.server.reset;

import cc.blynk.server.internal.token.ResetPassToken;
import cc.blynk.server.internal.token.TokensPool;
import cc.blynk.utils.AppNameUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class TokensPoolTest {

    @Test
    public void addTokenTest() {
        final ResetPassToken user = new ResetPassToken("test.gmail.com", AppNameUtil.BLYNK);
        final String token = "123";
        final TokensPool tokensPool = new TokensPool("");
        tokensPool.addToken(token, user);
        assertEquals(user, tokensPool.getBaseToken(token));
    }

    @Test
    public void addTokenTwiceTest() {
        final ResetPassToken user = new ResetPassToken("test.gmail.com", AppNameUtil.BLYNK);
        final String token = "123";
        final TokensPool tokensPool = new TokensPool("");
        tokensPool.addToken(token, user);
        tokensPool.addToken(token, user);
        assertEquals(1, tokensPool.size());
    }

    @Test
    public void remoteTokenTest() {
        final ResetPassToken user = new ResetPassToken("test.gmail.com", AppNameUtil.BLYNK);
        final String token = "123";
        final TokensPool tokensPool = new TokensPool("");
        tokensPool.addToken(token, user);
        assertEquals(user, tokensPool.getBaseToken(token));
        tokensPool.removeToken(token);
        assertEquals(0, tokensPool.size());
        assertNull(tokensPool.getBaseToken(token));
    }
}

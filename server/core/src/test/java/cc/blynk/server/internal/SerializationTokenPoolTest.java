package cc.blynk.server.internal;

import cc.blynk.server.internal.token.BaseToken;
import cc.blynk.server.internal.token.ResetPassToken;
import cc.blynk.server.internal.token.TokensPool;
import cc.blynk.utils.TokenGeneratorUtil;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SerializationTokenPoolTest {

    @Test
    public void someTEst() {
        String path = System.getProperty("java.io.tmpdir");
        TokensPool tokensPool = new TokensPool(path);

        String token = TokenGeneratorUtil.generateNewToken();
        ResetPassToken resetPassToken = new ResetPassToken("dima@mail.us", "Blynk");
        tokensPool.addToken(token, resetPassToken);
        tokensPool.close();

        TokensPool tokensPool2 = new TokensPool(path);
        ConcurrentHashMap<String, BaseToken> tokens = tokensPool2.getTokens();
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        ResetPassToken resetPassToken2 = (ResetPassToken) tokens.get(token);
        assertNotNull(resetPassToken2);
        assertEquals("dima@mail.us", resetPassToken2.email);
        assertEquals("Blynk", resetPassToken2.appName);
    }

}

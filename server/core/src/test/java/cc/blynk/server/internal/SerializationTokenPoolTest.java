package cc.blynk.server.internal;

import cc.blynk.server.internal.token.TokenUser;
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
        TokensPool tokensPool = new TokensPool(path, 1000_000);

        String token = TokenGeneratorUtil.generateNewToken();
        TokenUser tokenUser = new TokenUser("dima@mail.us", "Blynk");
        tokensPool.addToken(token, tokenUser);
        tokensPool.close();

        TokensPool tokensPool2 = new TokensPool(path, 1000_000);
        ConcurrentHashMap<String, TokenUser> tokens = tokensPool2.getTokens();
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        TokenUser tokenUser2 = tokens.get(token);
        assertNotNull(tokenUser2);
        assertEquals("dima@mail.us", tokenUser2.email);
        assertEquals("Blynk", tokenUser2.appName);
        assertEquals(System.currentTimeMillis(), tokenUser2.createdAt, 5000L);

    }

}

package cc.blynk.integration;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;

import java.security.Security;
import java.util.concurrent.atomic.AtomicLong;

public abstract class CounterBase {

    private static final String DEFAULT_TEST_USER = "dima@mail.ua";
    private static final AtomicLong userCounter = new AtomicLong();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    //generates unique name of a user, so every test is independent from others
    //name is unique only within the test
    public static String getUserName() {
        return userCounter.get() + DEFAULT_TEST_USER;
    }

    protected static String incrementAndGetUserName() {
        return userCounter.incrementAndGet() + DEFAULT_TEST_USER;
    }

    @Before
    public void incrementCounter() {
        userCounter.incrementAndGet();
    }

    public String getDataFolder() {
        return TestUtil.getDataFolder();
    }

}

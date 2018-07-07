package cc.blynk.integration;

import cc.blynk.server.hardware.handlers.hardware.logic.BlynkInternalLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.SetWidgetPropertyLogic;
import org.apache.commons.lang3.reflect.FieldUtils;
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
    public void incrementCounter() throws Exception {
        userCounter.incrementAndGet();

        //todo
        //yes, this is ugly hack. but right now this is fastest fix possible
        //otherwise we have to refactor all tests
        //right now, every test creates its own server for the test and own holder
        //problem with that is that we have singleton Logic handlers that are stored
        //in the static field, and this field initialized once on server start
        //and thus, those singleton holds always the same instance of holder
        //while it is recreated for the every test.
        //so we nullify static field holder before every test
        //so we always sure we don't use old references to the non-existing DAOs
        FieldUtils.writeDeclaredStaticField(SetWidgetPropertyLogic.class, "instance", null, true);
        FieldUtils.writeDeclaredStaticField(BlynkInternalLogic.class, "instance", null, true);
    }

    public String getDataFolder() {
        return TestUtil.getDataFolder();
    }

}

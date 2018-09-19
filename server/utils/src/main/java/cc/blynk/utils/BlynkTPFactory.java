package cc.blynk.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class BlynkTPFactory implements ThreadFactory  {

    private final ThreadFactory defaultThreadFactory;
    private final String name;

    private BlynkTPFactory(String name) {
        this.defaultThreadFactory = Executors.defaultThreadFactory();
        this.name = name;
    }

    public static ThreadFactory build(String name) {
        return new BlynkTPFactory(name);
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread thread = defaultThreadFactory.newThread(r);
        thread.setName(name + "-" + thread.getName());
        return thread;
    }
}

package cc.blynk.server.core.stats;

import java.util.concurrent.atomic.LongAdder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/13/2015.
 */
public class GlobalStats {

    private static final int LAST_COMMAND_INDEX = 72;
    //separate by income/outcome?
    public final Meter totalMessages;
    public final LongAdder[] specificCounters;

    public GlobalStats() {
        this.totalMessages = new Meter();

        //yeah, this is a bit ugly code, but as fast as possible =).
        this.specificCounters = new LongAdder[LAST_COMMAND_INDEX];
        for (int i = 0; i < LAST_COMMAND_INDEX; i++) {
            specificCounters[i] = new LongAdder();
        }
    }

    public void mark(final short cmd) {
        totalMessages.mark(1);
        specificCounters[cmd].increment();
    }

}

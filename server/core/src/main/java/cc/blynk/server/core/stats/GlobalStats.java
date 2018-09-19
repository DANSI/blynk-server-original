package cc.blynk.server.core.stats;

import cc.blynk.server.core.protocol.enums.Command;

import java.util.concurrent.atomic.LongAdder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/13/2015.
 */
public class GlobalStats {

    private static final int APP_STAT_COUNTER_INDEX = Command.LAST_COMMAND_INDEX - 1;
    private static final int MQTT_STAT_COUNTER_INDEX = Command.LAST_COMMAND_INDEX - 2;

    //separate by income/outcome?
    public final Meter totalMessages;

    //2 last load adders are used as separate counters
    public final LongAdder[] specificCounters;

    public GlobalStats() {
        this.totalMessages = new Meter();

        //yeah, this is a bit ugly code, but as fast as possible =).
        this.specificCounters = new LongAdder[Command.LAST_COMMAND_INDEX];
        for (int i = 0; i < Command.LAST_COMMAND_INDEX; i++) {
            specificCounters[i] = new LongAdder();
        }
    }

    public void markWithoutGlobal(short cmd) {
        specificCounters[cmd].increment();
    }

    public void mark(short cmd) {
        totalMessages.mark(1);
        markWithoutGlobal(cmd);
    }

    public void markSpecificCounterOnly(short cmd) {
        specificCounters[cmd].increment();
    }

    public void incrementAppStat() {
        specificCounters[APP_STAT_COUNTER_INDEX].increment();
    }

    public void incrementMqttStat() {
        specificCounters[MQTT_STAT_COUNTER_INDEX].increment();
    }

    public long getTotalAppCounter(boolean reset) {
        LongAdder longAdder = specificCounters[APP_STAT_COUNTER_INDEX];
        return reset ? longAdder.sumThenReset() : longAdder.sum();
    }

    public long getTotalMqttCounter(boolean reset) {
        LongAdder longAdder = specificCounters[MQTT_STAT_COUNTER_INDEX];
        return reset ? longAdder.sumThenReset() : longAdder.sum();
    }

}

package cc.blynk.common.stats;

import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.model.messages.protocol.BridgeMessage;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.model.messages.protocol.PingMessage;
import cc.blynk.common.model.messages.protocol.appllication.*;
import cc.blynk.common.model.messages.protocol.hardware.MailMessage;
import cc.blynk.common.model.messages.protocol.hardware.PushMessage;
import cc.blynk.common.model.messages.protocol.hardware.TweetMessage;
import com.codahale.metrics.Meter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/13/2015.
 */
public class GlobalStats {

    public final Meter incomeMessages;
    public final Map<Class<?>, LongAdder> specificCounters;

    public GlobalStats() {
        this.incomeMessages = new Meter();

        this.specificCounters = new HashMap<>();
        specificCounters.put(GetTokenMessage.class, new LongAdder());
        specificCounters.put(RefreshTokenMessage.class, new LongAdder());
        specificCounters.put(HardwareMessage.class, new LongAdder());
        specificCounters.put(LoadProfileMessage.class, new LongAdder());
        specificCounters.put(LoginMessage.class, new LongAdder());
        specificCounters.put(PingMessage.class, new LongAdder());
        specificCounters.put(RegisterMessage.class, new LongAdder());
        specificCounters.put(SaveProfileMessage.class, new LongAdder());
        specificCounters.put(ActivateDashboardMessage.class, new LongAdder());
        specificCounters.put(DeActivateDashboardMessage.class, new LongAdder());
        specificCounters.put(GetGraphDataMessage.class, new LongAdder());
        specificCounters.put(GetGraphDataResponseMessage.class, new LongAdder());
        specificCounters.put(TweetMessage.class, new LongAdder());
        specificCounters.put(MailMessage.class, new LongAdder());
        specificCounters.put(PushMessage.class, new LongAdder());
        specificCounters.put(ResponseMessage.class, new LongAdder());
        specificCounters.put(BridgeMessage.class, new LongAdder());
    }

    public void mark(Class<?> clazz) {
        incomeMessages.mark(1);
        specificCounters.get(clazz).increment();
    }

}

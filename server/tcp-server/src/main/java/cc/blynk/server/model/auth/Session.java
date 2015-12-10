package cc.blynk.server.model.auth;

import cc.blynk.common.model.messages.MessageBase;
import cc.blynk.server.exceptions.DeviceNotInNetworkException;
import cc.blynk.server.handlers.app.main.AppHandler;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.stats.metrics.InstanceLoadMeter;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.internal.ConcurrentSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static cc.blynk.server.utils.StateHolderUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 * 
 * DefaultChannelGroup.java too complicated. so doing in simple way for now.
 * 
 */
public class Session {

    private static final Logger log = LogManager.getLogger(Session.class);

    public final Set<Channel> appChannels = new ConcurrentSet<>();
    public final Set<Channel> hardwareChannels = new ConcurrentSet<>();

    public final EventLoop initialEventLoop;

    public Session(EventLoop initialEventLoop) {
        this.initialEventLoop = initialEventLoop;
    }

    public void sendMessageToHardware(Integer activeDashId, MessageBase message) {
        boolean noActiveHardware = true;
        for (Channel channel : hardwareChannels) {
            HardwareStateHolder hardwareState = getHardState(channel);
            if (hardwareState != null) {
                int dashId = hardwareState.dashId;
                if (activeDashId.equals(dashId)) {
                    noActiveHardware = false;
                    log.trace("Sending {} to hardware {}", message, channel);
                    channel.writeAndFlush(message);
                }
            }
        }
        if (noActiveHardware) {
            throw new DeviceNotInNetworkException(message.id);
        }
    }

    public boolean hasHardwareOnline(int activeDashId) {
        for (Channel channel : hardwareChannels) {
            HardwareStateHolder hardwareState = getHardState(channel);
            if (hardwareState != null) {
                if (hardwareState.dashId == activeDashId) {
                    return true;
                }
            }
        }
        return false;
    }

    public void sendMessageToHardware(MessageBase message) {
        for (Channel channel : hardwareChannels) {
            log.trace("Sending {} to hardware {}", message, channel);
            channel.writeAndFlush(message);
        }
    }

    public int getAppRequestRate() {
        double sum = 0;
        for (Channel c : appChannels) {
            AppHandler handler = c.pipeline().get(AppHandler.class);
            if (handler != null) {
                InstanceLoadMeter loadMeter = handler.getQuotaMeter();
                sum += loadMeter.getOneMinuteRateNoTick();
            }
        }
        return (int) sum;
    }

    public int getHardRequestRate() {
        double sum = 0;
        for (Channel c : hardwareChannels) {
            HardwareHandler handler = c.pipeline().get(HardwareHandler.class);
            if (handler != null) {
                InstanceLoadMeter loadMeter = handler.getQuotaMeter();
                sum += loadMeter.getOneMinuteRateNoTick();
            }
        }
        return (int) sum;
    }

    public void closeAll() {
        hardwareChannels.forEach(io.netty.channel.Channel::close);
        appChannels.forEach(io.netty.channel.Channel::close);
    }

}

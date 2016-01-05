package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.SyncMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.core.stats.metrics.InstanceLoadMeter;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.util.internal.ConcurrentSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static cc.blynk.utils.StateHolderUtil.*;

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

    private static int getRequestRate(Set<Channel> channels) {
        double sum = 0;
        for (Channel c : channels) {
            BaseSimpleChannelInboundHandler handler = c.pipeline().get(BaseSimpleChannelInboundHandler.class);
            if (handler != null) {
                InstanceLoadMeter loadMeter = handler.getQuotaMeter();
                sum += loadMeter.getOneMinuteRateNoTick();
            }
        }
        return (int) sum;
    }

    public static boolean needSync(Channel channel, String sharedToken) {
        BaseSimpleChannelInboundHandler appHandler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return appHandler != null && appHandler.state.contains(sharedToken);
    }

    public boolean sendMessageToHardware(int activeDashId, MessageBase message) {
        boolean noActiveHardware = true;
        for (Channel channel : hardwareChannels) {
            HardwareStateHolder hardwareState = getHardState(channel);
            if (hardwareState != null) {
                if (hardwareState.dashId == activeDashId) {
                    noActiveHardware = false;
                    log.trace("Sending {} to hardware {}", message, channel);
                    channel.writeAndFlush(message);
                }
            }
        }

        return noActiveHardware;
    }

    public void sendMessageToHardware(ChannelHandlerContext ctx, int activeDashId, MessageBase message) {
        if (sendMessageToHardware(activeDashId, message)) {
            log.debug("No device in session.");
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.DEVICE_NOT_IN_NETWORK));
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

    public void sendToApps(HardwareMessage message) {
        for (Channel channel : appChannels) {
            log.trace("Sending {} to app {}", message, channel);
            channel.writeAndFlush(message);
        }
    }

    public void sendToSharedApps(ChannelHandlerContext ctx, String sharedToken, SyncMessage message) {
        for (Channel appChannel : appChannels) {
            if (appChannel != ctx.channel() && needSync(appChannel, sharedToken)) {
                appChannel.writeAndFlush(message);
            }
        }
    }

    public int getAppRequestRate() {
        return getRequestRate(appChannels);
    }

    public int getHardRequestRate() {
        return getRequestRate(hardwareChannels);
    }

    public void closeAll() {
        hardwareChannels.forEach(io.netty.channel.Channel::close);
        appChannels.forEach(io.netty.channel.Channel::close);
    }

}

package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.SyncMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.core.stats.metrics.InstanceLoadMeter;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.*;
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
    public final EventLoop initialEventLoop;
    private final Set<Channel> appChannels = new ConcurrentSet<>();
    private final Set<Channel> hardwareChannels = new ConcurrentSet<>();
    private final ChannelFutureListener appRemover = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            removeAppChannel(future.channel());
        }
    };
    private final ChannelFutureListener hardRemover = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            removeHardChannel(future.channel());
        }
    };

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

    public void addAppChannel(Channel appChannel) {
        if (appChannels.add(appChannel)) {
            appChannel.closeFuture().addListener(appRemover);
        }
    }

    public void removeAppChannel(Channel appChannel) {
        if (appChannels.remove(appChannel)) {
            appChannel.closeFuture().removeListener(appRemover);
        }
    }

    public void addHardChannel(Channel hardChannel) {
        if (hardwareChannels.add(hardChannel)) {
            hardChannel.closeFuture().addListener(hardRemover);
        }
    }

    public void removeHardChannel(Channel hardChannel) {
        if (hardwareChannels.remove(hardChannel)) {
            hardChannel.closeFuture().removeListener(hardRemover);
        }
    }

    public boolean sendMessageToHardware(int activeDashId, MessageBase message) {
        boolean noActiveHardware = true;
        for (Channel channel : hardwareChannels) {
            HardwareStateHolder hardwareState = getHardState(channel);
            if (hardwareState != null) {
                if (hardwareState.dashId == activeDashId) {
                    noActiveHardware = false;
                    log.trace("Sending {} to hardware {}", message, channel);
                    channel.writeAndFlush(message, channel.voidPromise());
                }
            }
        }

        return noActiveHardware;
    }

    public void sendMessageToHardware(ChannelHandlerContext ctx, int activeDashId, MessageBase message) {
        if (sendMessageToHardware(activeDashId, message)) {
            log.debug("No device in session.");
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.DEVICE_NOT_IN_NETWORK), ctx.voidPromise());
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

    public void sendToApps(MessageBase message) {
        for (Channel channel : appChannels) {
            log.trace("Sending {} to app {}", message, channel);
            channel.writeAndFlush(message, channel.voidPromise());
        }
    }

    public void sendToSharedApps(ChannelHandlerContext ctx, String sharedToken, SyncMessage message) {
        for (Channel appChannel : appChannels) {
            if (appChannel != ctx.channel() && needSync(appChannel, sharedToken)) {
                appChannel.writeAndFlush(message, appChannel.voidPromise());
            }
        }
    }

    public int getAppRequestRate() {
        return getRequestRate(appChannels);
    }

    public int getHardRequestRate() {
        return getRequestRate(hardwareChannels);
    }

    public Set<Channel> getAppChannels() {
        return appChannels;
    }

    public Set<Channel> getHardwareChannels() {
        return hardwareChannels;
    }

    public void closeAll() {
        hardwareChannels.forEach(io.netty.channel.Channel::close);
        appChannels.forEach(io.netty.channel.Channel::close);
    }

}

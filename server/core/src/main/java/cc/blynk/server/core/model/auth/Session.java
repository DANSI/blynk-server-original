package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.protocol.model.messages.appllication.DeviceOfflineMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.core.stats.metrics.InstanceLoadMeter;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.utils.ArrayUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.util.internal.ConcurrentSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static cc.blynk.server.internal.BlynkByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.StateHolderUtil.getHardState;
import static cc.blynk.server.internal.StateHolderUtil.isSameDash;
import static cc.blynk.server.internal.StateHolderUtil.isSameDashAndDeviceId;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 * <p>
 * DefaultChannelGroup.java too complicated. so doing in simple way for now.
 */
public class Session {

    private static final Logger log = LogManager.getLogger(Session.class);

    public final EventLoop initialEventLoop;
    public final Set<Channel> appChannels = new ConcurrentSet<>();
    public final Set<Channel> hardwareChannels = new ConcurrentSet<>();

    private final ChannelFutureListener appRemover = future -> removeAppChannel(future.channel());
    private final ChannelFutureListener hardRemover = future -> removeHardChannel(future.channel());

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
        return appHandler != null && appHandler.getState().contains(sharedToken);
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

    private Set<Channel> filter(int activeDashId, int[] deviceIds) {
        Set<Channel> targetChannels = new HashSet<>();
        for (Channel channel : hardwareChannels) {
            HardwareStateHolder hardwareState = getHardState(channel);
            if (hardwareState != null && hardwareState.dash.id == activeDashId
                    && (deviceIds.length == 0 || ArrayUtil.contains(deviceIds, hardwareState.device.id))) {
                targetChannels.add(channel);
            }
        }
        return targetChannels;
    }

    private Set<Channel> filter(int activeDashId, int deviceId) {
        Set<Channel> targetChannels = new HashSet<>();
        for (Channel channel : hardwareChannels) {
            if (isSameDashAndDeviceId(channel, activeDashId, deviceId)) {
                targetChannels.add(channel);
            }
        }
        return targetChannels;
    }

    public boolean sendMessageToHardware(int activeDashId, short cmd, int msgId, String body, int deviceId) {
        return hardwareChannels.size() == 0 || sendMessageToHardware(filter(activeDashId, deviceId), cmd, msgId, body);
    }

    public boolean sendMessageToHardware(int activeDashId, short cmd, int msgId, String body, int... deviceIds) {
        return hardwareChannels.size() == 0 || sendMessageToHardware(filter(activeDashId, deviceIds), cmd, msgId, body);
    }

    public boolean sendMessageToHardware(short cmd, int msgId, String body) {
        return sendMessageToHardware(hardwareChannels, cmd, msgId, body);
    }

    private boolean sendMessageToHardware(Set<Channel> targetChannels, short cmd, int msgId, String body) {
        int channelsNum = targetChannels.size();
        if (channelsNum == 0) {
            return true; // -> no active hardware
        }

        send(targetChannels, channelsNum, cmd, msgId, body);

        return false; // -> there is active hardware
    }

    public boolean isHardwareConnected() {
        return hardwareChannels.size() > 0;
    }

    public boolean isHardwareConnected(int dashId, int deviceId) {
        for (Channel channel : hardwareChannels) {
            if (isSameDashAndDeviceId(channel, dashId, deviceId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isHardwareConnected(int dashId) {
        for (Channel channel : hardwareChannels) {
            if (isSameDash(channel, dashId)) {
                return true;
            }
        }
        return false;
    }

    public void sendOfflineMessageToApps(int dashId, int deviceId) {
        if (isAppConnected()) {
            log.trace("Sending device offline message.");

            //todo could be optimized. don't forget about retain
            DeviceOfflineMessage deviceOfflineMessage =
                    new DeviceOfflineMessage(0, String.valueOf(dashId) + DEVICE_SEPARATOR + deviceId);
            for (Channel appChannel : appChannels) {
                if (appChannel.isWritable()) {
                    appChannel.writeAndFlush(deviceOfflineMessage, appChannel.voidPromise());
                }
            }
        }
    }

    public void sendToApps(short cmd, int msgId, int dashId, int deviceId) {
        if (isAppConnected()) {
            String finalBody = "" + dashId + DEVICE_SEPARATOR + deviceId;
            sendToApps(cmd, msgId, dashId, finalBody);
        }
    }

    public void sendToApps(short cmd, int msgId, int dashId, int deviceId, String body) {
        if (isAppConnected()) {
            String finalBody = prependDashIdAndDeviceId(dashId, deviceId, body);
            sendToApps(cmd, msgId, dashId, finalBody);
        }
    }

    private void sendToApps(short cmd, int msgId, int dashId, String finalBody) {
        Set<Channel> targetChannels = filterByDash(dashId);

        int targetsNum = targetChannels.size();
        if (targetsNum > 0) {
            send(targetChannels, targetsNum, cmd, msgId, finalBody);
        }
    }

    private Set<Channel> filterByDash(int dashId) {
        Set<Channel> targetChannels = new HashSet<>();
        for (Channel channel : appChannels) {
            if (isSameDash(channel, dashId)) {
                targetChannels.add(channel);
            }
        }
        return targetChannels;
    }

    private void send(Set<Channel> targets, int targetsNum, short cmd, int msgId, String body) {
        ByteBuf msg = makeUTF8StringMessage(cmd, msgId, body);
        if (targetsNum > 1) {
            msg.retain(targetsNum - 1);
        }

        for (Channel channel : targets) {
            if (channel.isWritable()) {
                log.trace("Sending {} to channel {}", body, channel);
                channel.writeAndFlush(msg, channel.voidPromise());
            } else {
                msg.release();
            }
            msg.resetReaderIndex();
        }
    }

    public void sendToSharedApps(Channel sendingChannel, String sharedToken, short cmd, int msgId, String body) {
        Set<Channel> targetChannels = new HashSet<>();
        for (Channel channel : appChannels) {
            if (channel != sendingChannel && needSync(channel, sharedToken)) {
                targetChannels.add(channel);
            }
        }

        int channelsNum = targetChannels.size();
        if (channelsNum > 0) {
            send(targetChannels, channelsNum, cmd, msgId, body);
        }
    }

    public boolean isAppConnected() {
        return appChannels.size() > 0;
    }

    public int getAppRequestRate() {
        return getRequestRate(appChannels);
    }

    public int getHardRequestRate() {
        return getRequestRate(hardwareChannels);
    }

    public void closeHardwareChannelByDeviceId(int dashId, int deviceId) {
        for (Channel channel : hardwareChannels) {
            if (isSameDashAndDeviceId(channel, dashId, deviceId)) {
                channel.close();
            }
        }
    }

    public void closeHardwareChannelByDashId(int dashId) {
        for (Channel channel : hardwareChannels) {
            if (isSameDash(channel, dashId)) {
                channel.close();
            }
        }
    }

    public void closeAll() {
        hardwareChannels.forEach(io.netty.channel.Channel::close);
        appChannels.forEach(io.netty.channel.Channel::close);
    }

}

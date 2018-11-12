package cc.blynk.server.core.model.auth;

import cc.blynk.server.common.BaseSimpleChannelInboundHandler;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.decoders.MobileMessageDecoder;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.internal.CommonByteBufUtil.deviceOffline;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.StateHolderUtil.getHardState;
import static cc.blynk.server.internal.StateHolderUtil.isSameDash;
import static cc.blynk.server.internal.StateHolderUtil.isSameDashAndDeviceId;
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
    public final Set<Channel> appChannels = ConcurrentHashMap.newKeySet();
    public final Set<Channel> hardwareChannels = ConcurrentHashMap.newKeySet();

    private final ChannelFutureListener appRemover = future -> appChannels.remove(future.channel());
    private final ChannelFutureListener hardRemover = future -> hardwareChannels.remove(future.channel());

    public Session(EventLoop initialEventLoop) {
        this.initialEventLoop = initialEventLoop;
    }

    public boolean isSameEventLoop(ChannelHandlerContext ctx) {
        return isSameEventLoop(ctx.channel());
    }

    public boolean isSameEventLoop(Channel channel) {
        return initialEventLoop == channel.eventLoop();
    }

    private static int getRequestRate(Set<Channel> channels) {
        double sum = 0;
        for (Channel ch : channels) {
            MessageDecoder messageDecoder = ch.pipeline().get(MessageDecoder.class);
            if (messageDecoder != null) {
                sum += messageDecoder.getQuotaMeter().getOneMinuteRateNoTick();
            } else {
                MobileMessageDecoder mobileMessageDecoder = ch.pipeline().get(MobileMessageDecoder.class);
                if (mobileMessageDecoder != null) {
                    sum += mobileMessageDecoder.getQuotaMeter().getOneMinuteRateNoTick();
                }
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

    public void addHardChannel(Channel hardChannel) {
        if (hardwareChannels.add(hardChannel)) {
            hardChannel.closeFuture().addListener(hardRemover);
        }
    }

    private Set<Channel> filter(int bodySize, int activeDashId, int[] deviceIds) {
        Set<Channel> targetChannels = new HashSet<>();
        for (Channel channel : hardwareChannels) {
            HardwareStateHolder hardwareState = getHardState(channel);
            if (hardwareState != null && hardwareState.dash.id == activeDashId
                    && (deviceIds.length == 0 || ArrayUtil.contains(deviceIds, hardwareState.device.id))) {
                if (hardwareState.device.fitsBufferSize(bodySize)) {
                    targetChannels.add(channel);
                } else {
                    log.trace("Message is to large. Size {}.", bodySize);
                }
            }
        }
        return targetChannels;
    }

    private Set<Channel> filter(int bodySize, int activeDashId, int deviceId) {
        Set<Channel> targetChannels = new HashSet<>();
        for (Channel channel : hardwareChannels) {
            HardwareStateHolder hardwareState = getHardState(channel);
            if (hardwareState != null && hardwareState.isSameDashAndDeviceId(activeDashId, deviceId)) {
                if (hardwareState.device.fitsBufferSize(bodySize)) {
                    targetChannels.add(channel);
                } else {
                    log.trace("Message is to large. Size {}.", bodySize);
                }
            }
        }
        return targetChannels;
    }

    public boolean sendMessageToHardware(int activeDashId, short cmd, int msgId, String body, int deviceId) {
        return hardwareChannels.size() == 0
                || sendMessageToHardware(filter(body.length(), activeDashId, deviceId), cmd, msgId, body);
    }

    public boolean sendMessageToHardware(int activeDashId, short cmd, int msgId, String body, int... deviceIds) {
        return hardwareChannels.size() == 0
                || sendMessageToHardware(filter(body.length(), activeDashId, deviceIds), cmd, msgId, body);
    }

    public boolean sendMessageToHardware(short cmd, int msgId, String body) {
        return sendMessageToHardware(hardwareChannels, cmd, msgId, body);
    }

    private boolean sendMessageToHardware(Set<Channel> targetChannels, short cmd, int msgId, String body) {
        int channelsNum = targetChannels.size();
        if (channelsNum == 0) {
            return true; // -> no active hardware
        }

        send(targetChannels, cmd, msgId, body);

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
        int targetsNum = appChannels.size();
        if (targetsNum > 0) {
            log.trace("Sending device offline message.");

            StringMessage deviceOfflineMessage = deviceOffline(dashId, deviceId);
            sendMessageToMultipleReceivers(appChannels, deviceOfflineMessage);
        }
    }

    public void sendToApps(short cmd, int msgId, int dashId, int deviceId, String body) {
        if (isAppConnected()) {
            String finalBody = prependDashIdAndDeviceId(dashId, deviceId, body);
            sendToApps(cmd, msgId, dashId, finalBody);
        }
    }

    public void sendToApps(short cmd, int msgId, int dashId, String finalBody) {
        Set<Channel> targetChannels = filterByDash(dashId);

        int targetsNum = targetChannels.size();
        if (targetsNum > 0) {
            send(targetChannels, cmd, msgId, finalBody);
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

    private static void sendMessageToMultipleReceivers(Set<Channel> targets, StringMessage msg) {
        for (Channel channel : targets) {
            if (channel.isWritable()) {
                channel.writeAndFlush(msg, channel.voidPromise());
            }
        }
    }

    private static void send(Set<Channel> targets, short cmd, int msgId, String body) {
        StringMessage msg = makeUTF8StringMessage(cmd, msgId, body);
        sendMessageToMultipleReceivers(targets, msg);
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
            send(targetChannels, cmd, msgId, body);
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

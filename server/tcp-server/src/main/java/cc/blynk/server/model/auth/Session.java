package cc.blynk.server.model.auth;

import cc.blynk.common.model.messages.MessageBase;
import cc.blynk.server.exceptions.DeviceNotInNetworkException;
import io.netty.channel.Channel;
import io.netty.util.internal.ConcurrentSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static cc.blynk.server.utils.HandlerUtil.getState;

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

    public void sendMessageToHardware(Integer activeDashId, MessageBase message) {
        boolean noActiveHardware = true;
        for (Channel channel : hardwareChannels) {
            Integer dashId = getState(channel).dashId;
            if (activeDashId.equals(dashId)) {
                noActiveHardware = false;
                log.trace("Sending {} to hardware {}", message, channel);
                channel.writeAndFlush(message);
            }
        }
        if (noActiveHardware) {
            throw new DeviceNotInNetworkException(message.id);
        }
    }

    public void sendMessageToHardware(MessageBase message) {
        for (Channel channel : hardwareChannels) {
            log.trace("Sending {} to hardware {}", message, channel);
            channel.writeAndFlush(message);
        }
    }

    public void sendMessageToApp(MessageBase message) {
        for (Channel channel : appChannels) {
            log.trace("Sending {} to app {}", message, channel);
            channel.writeAndFlush(message);
        }
    }

}

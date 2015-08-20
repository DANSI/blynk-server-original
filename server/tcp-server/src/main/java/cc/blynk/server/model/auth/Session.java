package cc.blynk.server.model.auth;

import cc.blynk.common.model.messages.MessageBase;
import io.netty.channel.Channel;
import io.netty.util.internal.ConcurrentSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

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
        for (Channel channel : hardwareChannels) {
            Integer dashId = channel.attr(ChannelState.DASH_ID).get();
            if (dashId.equals(activeDashId)) {
                log.trace("Sending {} to {}", message, channel);
                channel.writeAndFlush(message);
            }
        }
    }

    public void sendMessageToHardware(MessageBase message) {
        for (Channel channel : hardwareChannels) {
            log.trace("Sending {} to {}", message, channel);
            channel.writeAndFlush(message);
        }
    }

    public void sendMessageToApp(MessageBase message) {
        for (Channel channel : appChannels) {
            log.trace("Sending {} to {}", message, channel);
            channel.writeAndFlush(message);
        }
    }

}

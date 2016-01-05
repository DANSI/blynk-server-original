package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.hardware.handlers.hardware.auth.HardwareProfile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 *
 * Simple handler that accepts info command from hardware.
 * At the moment only 1 param is used "h-beat".
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareInfoLogic {

    private static final Logger log = LogManager.getLogger(HardwareInfoLogic.class);

    private final int hardwareIdleTimeout;

    public HardwareInfoLogic(ServerProperties props) {
        this.hardwareIdleTimeout = props.getIntProperty("hard.socket.idle.timeout", 0);
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        String[] messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        HardwareProfile hardwareProfile = new HardwareProfile(messageParts);
        int newHardwareInterval = hardwareProfile.getHeartBeatInterval();

        log.trace("Info command. heartbeat interval {}", newHardwareInterval);

        if (hardwareIdleTimeout != 0 && newHardwareInterval > 0) {
            final int newReadTimeout = (int) Math.ceil(newHardwareInterval * 2.3D);
            log.trace("Changing read timeout interval to {}", newReadTimeout);
            ctx.pipeline().remove(ReadTimeoutHandler.class);
            ctx.pipeline().addFirst(new ReadTimeoutHandler(newReadTimeout));
        }

        ctx.writeAndFlush(produce(message.id, OK));
    }

}

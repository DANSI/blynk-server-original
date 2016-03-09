package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.StateHolderUtil.*;

/**
 * Bridge handler responsible for forwarding messages between different hardware via Blynk Server.
 * SendTo device defined by Auth Token.
 *
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class BridgeLogic {

    private final SessionDao sessionDao;
    private final Map<String, String> sendToMap;

    public BridgeLogic(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
        this.sendToMap = new ConcurrentHashMap<>();
    }

    private static boolean isInit(String body) {
        return body.length() > 0 && body.charAt(0) == 'i';
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        Session session = sessionDao.userSession.get(state.user);
        String[] split = message.body.split("\0");
        if (split.length < 3) {
            throw new IllegalCommandException("Wrong bridge body.", message.id);
        }
        if (isInit(split[1])) {
            final String pin = split[0];
            final String token = split[2];

            sendToMap.put(pin, token);

            ctx.writeAndFlush(new ResponseMessage(message.id, OK), ctx.voidPromise());
        } else {
            if (sendToMap.size() == 0) {
                throw new NotAllowedException("Bridge not initialized.", message.id);
            }

            final String pin = split[0];
            final String token = sendToMap.get(pin);

            if (session.getHardwareChannels().size() > 1) {
                boolean messageWasSent = false;
                message.body = message.body.substring(message.body.indexOf("\0") + 1);
                for (Channel channel : session.getHardwareChannels()) {
                    HardwareStateHolder hardwareState = getHardState(channel);
                    if (hardwareState != null && token.equals(hardwareState.token) && channel != ctx.channel()) {
                        messageWasSent = true;
                        channel.writeAndFlush(message, channel.voidPromise());
                    }
                }
                if (!messageWasSent) {
                    ctx.writeAndFlush(new ResponseMessage(message.id, Response.DEVICE_NOT_IN_NETWORK), ctx.voidPromise());
                }
            } else {
                ctx.writeAndFlush(new ResponseMessage(message.id, Response.DEVICE_NOT_IN_NETWORK), ctx.voidPromise());
            }
        }
    }
}


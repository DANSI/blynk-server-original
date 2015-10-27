package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.enums.Response;
import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.model.auth.Session;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;
import static cc.blynk.server.utils.StateHolderUtil.getHardState;

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

            ctx.writeAndFlush(produce(message.id, OK));
        } else {
            if (sendToMap.size() == 0) {
                throw new NotAllowedException("Bridge not initialized.", message.id);
            }

            final String pin = split[0];
            final String token = sendToMap.get(pin);

            if (session.hardwareChannels.size() > 1) {
                boolean messageWasSent = false;
                message.body = message.body.substring(message.body.indexOf("\0") + 1);
                for (Channel channel : session.hardwareChannels) {
                    if (token.equals(getHardState(channel).token) && channel != ctx.channel()) {
                        messageWasSent = true;
                        channel.writeAndFlush(message);
                    }
                }
                if (!messageWasSent) {
                    ctx.writeAndFlush(produce(message.id, Response.DEVICE_NOT_IN_NETWORK));
                }
            } else {
                ctx.writeAndFlush(produce(message.id, Response.DEVICE_NOT_IN_NETWORK));
            }
        }
    }
}


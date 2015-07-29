package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.enums.Response;
import cc.blynk.common.model.messages.protocol.BridgeMessage;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

public class BridgeLogic {

    private final SessionsHolder sessionsHolder;

    public BridgeLogic(SessionsHolder sessionsHolder) {
        this.sessionsHolder = sessionsHolder;
    }

    private static boolean isInit(String body) {
        return body.length() > 0 && body.charAt(0) == 'i';
    }

    //threadsafe
    private static Map<String, String> getOrInit(ChannelHandlerContext ctx) {
        Map<String, String> sendToMap = ctx.channel().attr(ChannelState.SEND_TO_TOKEN).get();
        if (sendToMap == null) {
            Map<String, String> newMap = new ConcurrentHashMap<>();
            sendToMap = ctx.channel().attr(ChannelState.SEND_TO_TOKEN).setIfAbsent(newMap);
            if (sendToMap == null) {
                return newMap;
            }
        }
        return sendToMap;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, BridgeMessage message) {
        Session session = sessionsHolder.userSession.get(user);
        String[] split = message.body.split("\0");
        if (split.length < 3) {
            throw new IllegalCommandException("Wrong bridge body.", message.id);
        }
        if (isInit(split[1])) {
            final String pin = split[0];
            final String token = split[2];

            Map<String, String> sendToMap = getOrInit(ctx);
            sendToMap.put(pin, token);

            ctx.writeAndFlush(produce(message.id, OK));
        } else {
            Map<String, String> sendToMap = ctx.channel().attr(ChannelState.SEND_TO_TOKEN).get();
            if (sendToMap == null || sendToMap.size() == 0) {
                throw new NotAllowedException("Bridge not initialized.", message.id);
            }

            final String pin = split[0];
            final String token = sendToMap.get(pin);

            if (session.hardwareChannels.size() > 1) {
                boolean messageWasSent = false;
                message.body = message.body.substring(message.body.indexOf("\0") + 1);
                for (Channel channel : session.hardwareChannels) {
                    if (token.equals(channel.attr(ChannelState.TOKEN).get()) && channel != ctx.channel()) {
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


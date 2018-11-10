package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.hardware.internal.BridgeForwardMessage;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static cc.blynk.server.core.protocol.enums.Command.BRIDGE;
import static cc.blynk.server.internal.CommonByteBufUtil.deviceNotInNetwork;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.server.internal.StateHolderUtil.getHardState;
import static cc.blynk.utils.StringUtils.split3;

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

    private static final Logger log = LogManager.getLogger(BridgeLogic.class);
    private final SessionDao sessionDao;
    private final TokenManager tokenManager;
    private Map<String, TokenValue> sendToMap;

    public BridgeLogic(SessionDao sessionDao, TokenManager tokenManager) {
        this.sessionDao = sessionDao;
        this.tokenManager = tokenManager;
    }

    private static boolean isInit(String body) {
        return body.length() > 0 && body.charAt(0) == 'i';
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        var session = sessionDao.get(state.userKey);
        var split = split3(message.body);

        if (split.length < 3) {
            log.error("Wrong bridge body. '{}'", message.body);
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        var bridgePin = split[0];

        if (isInit(split[1])) {
            var token = split[2];
            if (sendToMap == null) {
                sendToMap = new HashMap<>();
            }

            if (sendToMap.size() > 100 || token.length() != 32) {
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            //sendToMap may be already initialized, so checking it first.
            var tokenValue = sendToMap.get(token);
            if (tokenValue == null) {
                tokenValue = tokenManager.getTokenValueByToken(token);
                if (tokenValue == null) {
                    log.debug("Token {} for bridge command does not exists.", token);
                    ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                    return;
                }
            }

            if (!tokenValue.user.equals(state.user)) {
                log.debug("User {} allowed to access devices only within own account.", state.user);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            sendToMap.put(bridgePin, tokenValue);
            ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
        } else {
            if (sendToMap == null || sendToMap.size() == 0) {
                log.debug("Bridge not initialized. {}", state.user.email);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            var tokenvalue = sendToMap.get(bridgePin);
            if (tokenvalue == null) {
                log.debug("No token. Bridge not initialized. {}", state.user.email);
                ctx.writeAndFlush(notAllowed(message.id), ctx.voidPromise());
                return;
            }

            var body = message.body.substring(message.body.indexOf(StringUtils.BODY_SEPARATOR_STRING) + 1);
            var bridgeMessage = new StringMessage(message.id, BRIDGE, body);

            var targetDeviceId = tokenvalue.device.id;
            var targetDashId = tokenvalue.dash.id;

            if (session.hardwareChannels.size() > 1) {
                var messageWasSent = false;
                for (Channel channel : session.hardwareChannels) {
                    if (channel != ctx.channel() && channel.isWritable()) {
                        HardwareStateHolder hardwareState = getHardState(channel);
                        if (hardwareState != null
                                && hardwareState.isSameDashAndDeviceId(targetDashId, targetDeviceId)) {
                            messageWasSent = true;
                            channel.writeAndFlush(bridgeMessage, channel.voidPromise());
                        }
                    }
                }
                if (!messageWasSent) {
                    ctx.writeAndFlush(deviceNotInNetwork(message.id), ctx.voidPromise());
                }
            } else {
                ctx.writeAndFlush(deviceNotInNetwork(message.id), ctx.voidPromise());
            }

            ctx.pipeline().fireUserEventTriggered(new BridgeForwardMessage(bridgeMessage, tokenvalue, state.userKey));
        }
    }
}


package cc.blynk.server.hardware.handlers.hardware.mqtt.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.hardware.handlers.hardware.MqttHardwareHandler;
import cc.blynk.server.internal.ReregisterChannelUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_CONNECTED;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;

/**
 * Handler responsible for managing hardware and apps login messages.
 * Initializes netty channel with a state tied with user.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class MqttHardwareLoginHandler extends SimpleChannelInboundHandler<MqttConnectMessage> {

    private static final Logger log = LogManager.getLogger(MqttHardwareLoginHandler.class);

    private static final MqttConnAckMessage ACCEPTED = createConnAckMessage(MqttConnectReturnCode.CONNECTION_ACCEPTED);

    private final Holder holder;

    public MqttHardwareLoginHandler(Holder holder) {
        this.holder = holder;
    }

    private static void completeLogin(Channel channel, Session session, User user,
                                      DashBoard dash, Device device, int msgId) {
        log.debug("completeLogin. {}", channel);

        session.addHardChannel(channel);
        channel.writeAndFlush(ACCEPTED);

        String responseBody = String.valueOf(dash.id) + DEVICE_SEPARATOR + device.id;
        session.sendToApps(HARDWARE_CONNECTED, msgId, dash.id, responseBody);

        log.info("{} mqtt hardware joined.", user.email);
    }

    private static MqttConnAckMessage createConnAckMessage(MqttConnectReturnCode code) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(code, true);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttConnectMessage message) {
        MqttConnectPayload mqttConnectPayload = message.payload();
        if (mqttConnectPayload == null) {
            ctx.writeAndFlush(createConnAckMessage(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD), ctx.voidPromise());
            return;
        }

        String username = mqttConnectPayload.userName();
        if (username == null) {
            ctx.writeAndFlush(createConnAckMessage(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD), ctx.voidPromise());
            return;
        }

        username = username.trim().toLowerCase();
        byte[] password = mqttConnectPayload.passwordInBytes();
        if (password == null) {
            ctx.writeAndFlush(createConnAckMessage(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD), ctx.voidPromise());
            return;
        }

        String token = new String(password, CharsetUtil.UTF_8);

        TokenValue tokenValue = holder.tokenManager.getTokenValueByToken(token);

        if (tokenValue == null || !tokenValue.user.email.equalsIgnoreCase(username)) {
            log.debug("MqttHardwareLogic token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(createConnAckMessage(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD), ctx.voidPromise());
            return;
        }

        User user = tokenValue.user;
        Device device = tokenValue.device;
        DashBoard dash = tokenValue.dash;

        ChannelPipeline pipeline = ctx.pipeline();
        HardwareStateHolder hardwareStateHolder = new HardwareStateHolder(user, tokenValue.dash, device);
        pipeline.replace(this, "HHArdwareMqttHandler", new MqttHardwareHandler(holder, hardwareStateHolder));

        Session session = holder.sessionDao.getOrCreateSessionByUser(
                hardwareStateHolder.userKey, ctx.channel().eventLoop());

        if (session.isSameEventLoop(ctx)) {
            completeLogin(ctx.channel(), session, user, dash, device, -1);
        } else {
            log.debug("Re registering hard channel. {}", ctx.channel());
            ReregisterChannelUtil.reRegisterChannel(ctx, session, channelFuture ->
                    completeLogin(channelFuture.channel(), session, user, dash, device, -1));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        DefaultExceptionHandler.handleGeneralException(ctx, cause);
    }

}

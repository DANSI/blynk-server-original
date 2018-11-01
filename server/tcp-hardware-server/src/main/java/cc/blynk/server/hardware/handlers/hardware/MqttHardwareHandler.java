package cc.blynk.server.hardware.handlers.hardware;

import cc.blynk.server.Holder;
import cc.blynk.server.common.BaseSimpleChannelInboundHandler;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.core.session.StateHolderBase;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.hardware.handlers.hardware.mqtt.logic.MqttHardwareLogic;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageFactory;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
public class MqttHardwareHandler extends BaseSimpleChannelInboundHandler<MqttMessage> {

    public final HardwareStateHolder state;
    private final MqttHardwareLogic hardware;
    private final GlobalStats stats;

    public MqttHardwareHandler(Holder holder, HardwareStateHolder stateHolder) {
        super(MqttMessage.class);
        this.hardware = new MqttHardwareLogic(holder.sessionDao, holder.reportingDiskDao);
        this.state = stateHolder;
        this.stats = holder.stats;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MqttMessage msg) {
        this.stats.incrementMqttStat();
        MqttMessageType messageType = msg.fixedHeader().messageType();

        switch (messageType) {
            case PUBLISH :
                MqttPublishMessage publishMessage = (MqttPublishMessage) msg;
                String topic = publishMessage.variableHeader().topicName();

                switch (topic.toLowerCase()) {
                    case "hardware" :
                        hardware.messageReceived(state, publishMessage);
                        break;
                }

                break;

            case PINGREQ :
                ctx.writeAndFlush(
                        MqttMessageFactory.newMessage(msg.fixedHeader(), msg.variableHeader(), null),
                        ctx.voidPromise());
                break;

            case DISCONNECT :
                log.trace("Got disconnect. Closing...");
                ctx.close();
                break;
        }
    }

    @Override
    public StateHolderBase getState() {
        return state;
    }
}

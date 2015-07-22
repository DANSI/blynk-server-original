package cc.blynk.server.handlers.common;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.exceptions.DeviceNotInNetworkException;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoActiveDashboardException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class HardwareHandler extends BaseSimpleChannelInboundHandler<HardwareMessage> {

    private final StorageDao storageDao;

    public HardwareHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder, StorageDao storageDao) {
        super(props, userRegistry, sessionsHolder);
        this.storageDao = storageDao;
    }

    private static boolean pinModeMessage(String body) {
        return body.length() > 0 && body.charAt(0) == 'p';
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, HardwareMessage message) {
        Session session = sessionsHolder.userSession.get(user);

        if (ctx.channel().attr(ChannelState.IS_HARD_CHANNEL).get()) {
            //if message from hardware, check if it belongs to graph. so we need save it in that case
            if (message.body.length() < 4) {
                throw new IllegalCommandException("Hardware command body too short.", message.id);
            }

            StoreMessage storeMessage = null;
            if (message.body.charAt(1) == 'w') {
                storeMessage = storageDao.process(user.getProfile(), ctx.channel().attr(ChannelState.DASH_ID).get(), message.body, message.id);
            }

            if (user.getProfile().activeDashId == null) {
                throw new NoActiveDashboardException(message.id);
            }

            if (session.appChannels.size() > 0) {
                if (storeMessage == null) {
                    session.sendMessageToApp(message);
                } else {
                    session.sendMessageToApp(message.updateMessageBody(storeMessage.toString()));
                }

            }
        } else {
            if (user.getProfile().activeDashId == null) {
                //throw new NoActiveDashboardException(message.id);
                return;
            }

            if (pinModeMessage(message.body)) {
                log.trace("Pin Mode message catch. Remembering.");
                //check PM command not empty
                if (message.body.length() > 3) {
                    user.getProfile().pinModeMessage = message;
                }
            }

            if (session.hardwareChannels.size() == 0) {
                throw new DeviceNotInNetworkException(message.id);
            }

            session.sendMessageToHardware(user.getProfile().activeDashId, message);
        }

    }

}

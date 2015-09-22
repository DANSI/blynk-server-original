package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoActiveDashboardException;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareLogic {

    private final StorageDao storageDao;
    private final SessionsHolder sessionsHolder;

    public HardwareLogic(SessionsHolder sessionsHolder, StorageDao storageDao) {
        this.sessionsHolder = sessionsHolder;
        this.storageDao = storageDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, HandlerState state, Message message) {
        Session session = sessionsHolder.userSession.get(state.user);

        //if message from hardware, check if it belongs to graph. so we need save it in that case
        if (message.body.length() < 4) {
            throw new IllegalCommandException("HardwareLogic command body too short.", message.id);
        }

        StoreMessage storeMessage = null;
        if (message.body.charAt(1) == 'w') {
            storeMessage = storageDao.process(state.user.profile, state.dashId, message.body);
        }

        if (state.user.profile.activeDashId == null || !state.user.profile.activeDashId.equals(state.dashId)) {
            throw new NoActiveDashboardException(message.id);
        }

        if (session.appChannels.size() > 0) {
            if (storeMessage == null) {
                session.sendMessageToApp(message);
            } else {
                session.sendMessageToApp(((HardwareMessage) message).updateMessageBody(message.body + StringUtils.BODY_SEPARATOR_STRING + storeMessage.ts));
            }

        }
    }

}

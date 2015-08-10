package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoActiveDashboardException;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
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

    public HardwareLogic(ServerProperties props, SessionsHolder sessionsHolder, StorageDao storageDao) {
        this.sessionsHolder = sessionsHolder;
        this.storageDao = storageDao;
        updateProperties(props);
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        Session session = sessionsHolder.userSession.get(user);

        //if message from hardware, check if it belongs to graph. so we need save it in that case
        if (message.body.length() < 4) {
            throw new IllegalCommandException("HardwareLogic command body too short.", message.id);
        }

        StoreMessage storeMessage = null;
        if (message.body.charAt(1) == 'w') {
            try {
                storeMessage = storageDao.process(user.getProfile(), ctx.channel().attr(ChannelState.DASH_ID).get(), message.body);
            } catch (NumberFormatException e) {
                throw new IllegalCommandException("HardwareLogic command body incorrect.", message.id);
            }
        }

        if (user.getProfile().activeDashId == null) {
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

    public void updateProperties(ServerProperties props) {
        if (storageDao != null) {
            storageDao.updateProperties(props);
        }
    }
}

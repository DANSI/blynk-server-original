package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.dao.graph.StoreMessage;
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
public class HardwareHardHandler extends BaseSimpleChannelInboundHandler<HardwareMessage> {

    private final StorageDao storageDao;

    public HardwareHardHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder, StorageDao storageDao) {
        super(props, userRegistry, sessionsHolder);
        this.storageDao = storageDao;
        updateProperties(props);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, HardwareMessage message) {
        Session session = sessionsHolder.userSession.get(user);

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
    }

    @Override
    public void updateProperties(ServerProperties props) {
        super.updateProperties(props);
        if (storageDao != null) {
            storageDao.updateProperties(props);
        }
    }
}

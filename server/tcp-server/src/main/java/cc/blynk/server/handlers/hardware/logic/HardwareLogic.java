package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoActiveDashboardException;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.graph.StoreMessage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Handler responsible for forwarding messages from hardware to applications.
 * Also handler stores all incoming hardware commands to disk in order to export and
 * analyze data.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareLogic {

    private final ReportingDao reportingDao;
    private final SessionDao sessionDao;

    public HardwareLogic(SessionDao sessionDao, ReportingDao reportingDao) {
        this.sessionDao = sessionDao;
        this.reportingDao = reportingDao;
    }

    private static String attachTS(String body, long ts) {
        return body + StringUtils.BODY_SEPARATOR_STRING + ts;
    }

    private static String attachDashId(String body, int dashId) {
        return dashId + StringUtils.BODY_SEPARATOR_STRING + body;
    }

    public void messageReceived(ChannelHandlerContext ctx, HandlerState state, Message message) {
        Session session = sessionDao.userSession.get(state.user);

        //if message from hardware, check if it belongs to graph. so we need save it in that case
        if (message.body.length() < 4) {
            throw new IllegalCommandException("HardwareLogic command body too short.", message.id);
        }

        String body = message.body;
        long ts = System.currentTimeMillis();

        final int dashId = state.dashId;

        if (body.charAt(1) == 'w') {
            GraphKey key = new GraphKey(dashId, body, ts);

            //storing to DB and aggregating
            storageDao.process(key);

            //in case message is for graph - attaching ts.
            if (state.user.profile.hasGraphPin(key)) {
                body = attachTS(body, ts);
            }
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

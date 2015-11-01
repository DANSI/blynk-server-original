package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoActiveDashboardException;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.graph.GraphKey;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.utils.StateHolderUtil.*;

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

    private static final Logger log = LogManager.getLogger(HardwareLogic.class);

    private final ReportingDao reportingDao;
    private final SessionDao sessionDao;

    public HardwareLogic(SessionDao sessionDao, ReportingDao reportingDao) {
        this.sessionDao = sessionDao;
        this.reportingDao = reportingDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        Session session = sessionDao.userSession.get(state.user);

        if (message.body.length() < 4) {
            throw new IllegalCommandException("HardwareLogic command body too short.", message.id);
        }

        String body = message.body;
        long ts = System.currentTimeMillis();

        final int dashId = state.dashId;

        if (body.charAt(1) == 'w') {
            GraphKey key = new GraphKey(dashId, body, ts);

            //storing to DB and aggregating
            reportingDao.process(state.user.name, key);

            //in case message is for graph - attaching ts.
            //todo remove this after adding support in apps
            if (state.user.profile.hasGraphPin(key)) {
                body += StringUtils.BODY_SEPARATOR_STRING + ts;
            }
        }

        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        if (!dash.isActive) {
            throw new NoActiveDashboardException(message.id);
        }

        if (session.appChannels.size() > 0) {
            //todo this code should be removed when both iOS and Android will support sharing.
            for (Channel channel : session.appChannels) {
                log.trace("Sending {} to app {}", message, channel);
                if (getAppState(channel).isOldAPI()) {
                    channel.writeAndFlush(new HardwareMessage(message.id, body));
                } else {
                    channel.writeAndFlush(new HardwareMessage(message.id, dashId + StringUtils.BODY_SEPARATOR_STRING + body));
                }
            }
        }
    }

}

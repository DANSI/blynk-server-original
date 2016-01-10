package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.HardwareBody;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.graph.GraphKey;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.PinUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        if (PinUtil.isWriteOperation(body)) {
            String[] bodyParts = body.split(StringUtils.BODY_SEPARATOR_STRING);
            GraphKey key = new GraphKey(dashId, bodyParts, ts);

            //storing to DB and aggregating
            reportingDao.process(state.user.name, key);

            //in case message is for graph - attaching ts.
            //todo remove this after adding support in apps
            if (state.user.profile.hasGraphPin(key)) {
                body += StringUtils.BODY_SEPARATOR_STRING + ts;
            }

            dash.update(new HardwareBody(body, message.id));
        }

        if (dash.isActive) {
            session.sendToApps(new HardwareMessage(message.id, dashId + StringUtils.BODY_SEPARATOR_STRING + body));
        } else {
            log.debug("No active dashboard.");
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.NO_ACTIVE_DASHBOARD));
        }
    }

}

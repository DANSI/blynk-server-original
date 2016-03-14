package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.ByteBufUtil.*;

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

    private static boolean isWriteOperation(String body) {
        return body.charAt(1) == 'w';
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        Session session = sessionDao.userSession.get(state.user);

        final String body = message.body;

        //minimum command - "ar 1"
        if (body.length() < 4) {
            throw new IllegalCommandException("HardwareLogic command body too short.", message.id);
        }

        int dashId = state.dashId;
        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        if (isWriteOperation(body)) {
            String[] splitBody = body.split(StringUtils.BODY_SEPARATOR_STRING);

            final PinType pinType = PinType.getPinType(splitBody[0].charAt(0));
            final byte pin = ParseUtil.parseByte(splitBody[1], message.id);
            final String[] values = Arrays.copyOfRange(splitBody, 2, splitBody.length);

            //storing to DB and aggregating
            if (values.length == 0) {
                throw new IllegalCommandException("Hardware write command doesn't have value for pin.", message.id);
            }

            reportingDao.process(state.user.name, dashId, pin, pinType, values);
            dash.update(pin, pinType, values);
        }

        if (dash.isActive) {
            session.sendToApps(HARDWARE, message.id, dashId + StringUtils.BODY_SEPARATOR_STRING + body);
        } else {
            log.debug("No active dashboard.");
            ctx.writeAndFlush(makeResponse(ctx, message.id, NO_ACTIVE_DASHBOARD), ctx.voidPromise());
        }
    }

}

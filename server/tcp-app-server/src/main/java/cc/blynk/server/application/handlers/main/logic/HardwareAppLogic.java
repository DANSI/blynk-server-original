package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.FrequencyWidget;
import cc.blynk.server.core.processors.WebhookProcessor;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.SYNC;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND_BODY;
import static cc.blynk.utils.BlynkByteBufUtil.makeResponse;
import static cc.blynk.utils.StringUtils.split2;
import static cc.blynk.utils.StringUtils.split2Device;
import static cc.blynk.utils.StringUtils.split3;

/**
 * Responsible for handling incoming hardware commands from applications and forwarding it to
 * appropriate hardware.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareAppLogic {

    private static final Logger log = LogManager.getLogger(HardwareAppLogic.class);

    private final SessionDao sessionDao;
    private final WebhookProcessor webhookProcessor;

    public HardwareAppLogic(Holder holder, String username) {
        this.sessionDao = holder.sessionDao;
        this.webhookProcessor = new WebhookProcessor(holder.asyncHttpClient,
                holder.props.getLongProperty("webhooks.frequency.user.quota.limit", 1000),
                holder.props.getIntProperty("webhooks.response.size.limit", 64),
                holder.stats,
                username);
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        Session session = sessionDao.userSession.get(state.userKey);

        String[] split = split2(message.body);

        String[] dashIdAndTargetIdString = split2Device(split[0]);
        int dashId = ParseUtil.parseInt(dashIdAndTargetIdString[0]);
        int targetId = 0;

        //new logic for multi devices
        if (dashIdAndTargetIdString.length == 2) {
            targetId = ParseUtil.parseInt(dashIdAndTargetIdString[1]);
        }

        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        //if no active dashboard - do nothing. this could happen only in case of app. bug
        if (!dash.isActive) {
            return;
        }

        //sending message only if widget assigned to device or tag has assigned devices
        int[] deviceIds = dash.getDeviceIdsByTarget(targetId);
        if (deviceIds == null) {
            log.debug("No assigned target id for received command.");
            return;
        }

        final char operation = split[1].charAt(1);
        switch (operation) {
            case 'w' :
                String[] splitBody = split3(split[1]);
                final PinType pinType = PinType.getPinType(splitBody[0].charAt(0));
                final byte pin = ParseUtil.parseByte(splitBody[1]);
                final String value = splitBody[2];

                dash.update(targetId, pin, pinType, value);

                //if dash was shared. check for shared channels
                if (state.user.dashShareTokens != null) {
                    String sharedToken = state.user.dashShareTokens.get(dashId);
                    session.sendToSharedApps(ctx.channel(), sharedToken, SYNC, message.id, message.body);
                }

                session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1], deviceIds);
                webhookProcessor.process(session, dash, targetId, pin, pinType, value);

                break;
            case 'r' :
                Widget widget = dash.findWidgetByPin(targetId, split[1].split(StringUtils.BODY_SEPARATOR_STRING));
                if (widget == null) {
                    log.debug("No widget for read command.");
                    ctx.writeAndFlush(makeResponse(message.id, ILLEGAL_COMMAND_BODY), ctx.voidPromise());
                    return;
                }

                if (widget instanceof FrequencyWidget) {
                    if (((FrequencyWidget) widget).isTicked(split[1])) {
                        session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1], targetId);
                    }
                } else {
                    //corner case for 3-d parties. sometimes users need to read pin state even from non-frequency widgets
                    session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1], targetId);
                }
                break;
        }
    }

}

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
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.SYNC;
import static cc.blynk.utils.StringUtils.split2;
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

    public HardwareAppLogic(Holder holder) {
        this.sessionDao = holder.sessionDao;
        this.webhookProcessor = new WebhookProcessor(holder.asyncHttpClient,
                holder.props.getLongProperty("webhooks.frequency.user.quota.limit", 1000),
                holder.stats);
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        Session session = sessionDao.userSession.get(state.user);

        String[] split = split2(message.body);
        int dashId = ParseUtil.parseInt(split[0]);

        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        //if no active dashboard - do nothing. this could happen only in case of app. bug
        if (!dash.isActive) {
            return;
        }

        final char operation = split[1].charAt(1);
        switch (operation) {
            case 'w' :
                String[] splitBody = split3(split[1]);
                final PinType pinType = PinType.getPinType(splitBody[0].charAt(0));
                final byte pin = ParseUtil.parseByte(splitBody[1]);
                final String value = splitBody[2];

                dash.update(pin, pinType, value);

                //if dash was shared. check for shared channels
                if (state.user.dashShareTokens != null) {
                    String sharedToken = state.user.dashShareTokens.get(dashId);
                    session.sendToSharedApps(ctx.channel(), sharedToken, SYNC, message.id, message.body);
                }
                session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1]);

                //todo this temp catch. remove in next update.
                try {
                    webhookProcessor.process(dash, pin, pinType, value);
                } catch (Exception e) {
                    log.error("Error app processing.", e);
                }
                break;
            case 'r' :
                Widget widget = dash.findWidgetByPin(split[1].split(StringUtils.BODY_SEPARATOR_STRING));
                if (widget == null) {
                    throw new IllegalCommandBodyException("No widget for read command.");
                }

                if (widget instanceof FrequencyWidget) {
                    if (((FrequencyWidget) widget).isTicked(split[1])) {
                        session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1]);
                    }
                } else {
                    //corner case for 3-d parties. sometimes users need to read pin state even from non-frequency widgets
                    session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1]);
                }
                break;
        }
    }

}

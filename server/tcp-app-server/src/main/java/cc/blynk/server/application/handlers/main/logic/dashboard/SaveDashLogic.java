package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class SaveDashLogic {

    private static final Logger log = LogManager.getLogger(SaveDashLogic.class);

    private final int DASH_MAX_SIZE;

    public SaveDashLogic(int maxDashSize) {
        this.DASH_MAX_SIZE = maxDashSize;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashString = message.body;

        //expecting message with 2 parts
        if (dashString == null || dashString.equals("")) {
            throw new IllegalCommandException("Income create dash message is empty.", message.id);
        }

        if (dashString.length() > DASH_MAX_SIZE) {
            throw new NotAllowedException("User dashboard is larger then limit.", message.id);
        }

        log.debug("Trying to parse user dash : {}", dashString);
        DashBoard updatedDash = JsonParser.parseDashboard(dashString, message.id);

        log.info("Saving dashboard.");

        int index = user.profile.getDashIndex(updatedDash.id, message.id);
        //do not accept isActive field from "saveDash" command
        updatedDash.isActive = user.profile.dashBoards[index].isActive;

        Notification newNotification = updatedDash.getWidgetByType(Notification.class);
        if (newNotification != null) {
            Notification oldNotification = user.profile.dashBoards[index].getWidgetByType(Notification.class);
            if (oldNotification != null) {
                newNotification.iOSTokens = oldNotification.iOSTokens;
                newNotification.androidTokens = oldNotification.androidTokens;
            }
        }

        user.profile.dashBoards[index] = updatedDash;
        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(new ResponseMessage(message.id, OK));
    }

}

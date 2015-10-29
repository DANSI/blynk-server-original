package cc.blynk.server.handlers.app.main.logic.reporting;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.exceptions.IllegalCommandBodyException;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.handlers.app.main.auth.AppStateHolder;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;
import static cc.blynk.server.utils.StateHolderUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetGraphDataLogic {

    private static final Logger log = LogManager.getLogger(GetGraphDataLogic.class);

    private static int VALUES_PER_PIN = 5;
    private final BlockingIOProcessor blockingIOProcessor;
    private final ReportingDao reportingDao;

    public GetGraphDataLogic(ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor) {
        this.reportingDao = reportingDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        //warn: split may be optimized
        String[] messageParts = message.body.split(" ");

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        //special case for delete command
        if (messageParts.length == 4) {
            deleteGraphData(messageParts, user.name, message.id);
            ctx.writeAndFlush(produce(message.id, OK));
        } else {
            //todo remove after next deployment
            AppStateHolder state = getAppState(ctx.pipeline());
            if (state.isOldAPI() || ("Android".equals(state.osType) && "21".equals(state.version))) {
                process(ctx.channel(), messageParts, user, message.id, VALUES_PER_PIN);
            } else {
                int dashId = ParseUtil.parseInt(messageParts[0], message.id);
                user.profile.validateDashId(dashId, message.id);
                processNewAPI(ctx.channel(), dashId, Arrays.copyOfRange(messageParts, 1, messageParts.length), user, message.id, 4);
            }
        }
    }

    private void process(Channel channel, String[] messageParts, User user, int msgId, int valuesPerPin) {
        int numberOfPins = messageParts.length / valuesPerPin;

        GraphPinRequest[] requestedPins = new GraphPinRequestData[numberOfPins];

        for (int i = 0; i < numberOfPins; i++) {
            requestedPins[i] = new GraphPinRequestData(messageParts, i, msgId, valuesPerPin);
            user.profile.validateDashId(requestedPins[i].dashId, msgId);
        }

        blockingIOProcessor.readGraphData(channel, user.name, requestedPins, msgId);
    }

    private void processNewAPI(Channel channel, int dashId, String[] messageParts, User user, int msgId, int valuesPerPin) {
        int numberOfPins = messageParts.length / valuesPerPin;

        GraphPinRequest[] requestedPins = new GraphPinRequestDataNewAPI[numberOfPins];

        for (int i = 0; i < numberOfPins; i++) {
            requestedPins[i] = new GraphPinRequestDataNewAPI(dashId, messageParts, i, msgId, valuesPerPin);
        }

        blockingIOProcessor.readGraphDataNewAPI(channel, user.name, requestedPins, msgId);
    }

    private void deleteGraphData(String[] messageParts, String username, int msgId) {
        try {
            int dashBoardId = Integer.parseInt(messageParts[0]);
            PinType pinType = PinType.getPingType(messageParts[1].charAt(0));
            byte pin = Byte.parseByte(messageParts[2]);
            String cmd = messageParts[3];
            if (!"del".equals(cmd)) {
                throw new IllegalCommandBodyException("Wrong body format. Expecting 'del'.", msgId);
            }
            reportingDao.delete(username, dashBoardId, pinType, pin);
        } catch (NumberFormatException e) {
            throw new IllegalCommandException("HardwareLogic command body incorrect.", msgId);
        }
    }

}

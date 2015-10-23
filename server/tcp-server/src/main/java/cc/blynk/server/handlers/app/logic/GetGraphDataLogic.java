package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.appllication.GetGraphDataResponseMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.exceptions.IllegalCommandBodyException;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoDataException;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.enums.GraphType;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.utils.HandlerUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;
import static cc.blynk.server.utils.ByteUtils.compress;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetGraphDataLogic {

    private static final Logger log = LogManager.getLogger(GetGraphDataLogic.class);

    private static int VALUES_PER_PIN = 5;
    private final ReportingDao reportingDao;

    public GetGraphDataLogic(ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
    }

    private static boolean checkNoData(byte[][] data) {
        boolean noData = true;

        for (byte[] pinData : data) {
            noData = noData && pinData.length == 0;
        }

        return noData;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
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
            HandlerState state = HandlerUtil.getState(ctx.channel());
            if (state.isOldAPI() || ("Android".equals(state.osType) && "21".equals(state.version))) {
                byte[][] data = process(messageParts, user, message.id, VALUES_PER_PIN);

                if (checkNoData(data)) {
                    throw new NoDataException(message.id);
                }

                log.trace("Sending getGraph response. ");
                byte[] compressed = compress(data, message.id);
                ctx.writeAndFlush(new GetGraphDataResponseMessage(message.id, compressed));
            } else {
                int dashId = ParseUtil.parseInt(messageParts[0], message.id);
                user.profile.validateDashId(dashId, message.id);
                byte[][] data = processNewAPI(dashId, Arrays.copyOfRange(messageParts, 1, messageParts.length), user, message.id, 4);

                if (checkNoData(data)) {
                    throw new NoDataException(message.id);
                }

                log.trace("Sending getGraph response. ");
                byte[] compressed = compress(dashId, data, message.id);
                ctx.writeAndFlush(new GetGraphDataResponseMessage(message.id, compressed));
            }
        }
    }

    private byte[][] process(String[] messageParts, User user, int msgId, int valuesPerPin) {
        int numberOfPins = messageParts.length / valuesPerPin;

        GraphPinRequest[] requestedPins = new GraphPinRequestData[numberOfPins];

        for (int i = 0; i < numberOfPins; i++) {
            requestedPins[i] = new GraphPinRequestData(messageParts, i, msgId, valuesPerPin);
            user.profile.validateDashId(requestedPins[i].dashId, msgId);
        }

        return reportingDao.getAllFromDisk(user.name, requestedPins);
    }

    private byte[][] processNewAPI(int dashId, String[] messageParts, User user, int msgId, int valuesPerPin) {
        int numberOfPins = messageParts.length / valuesPerPin;

        GraphPinRequest[] requestedPins = new GraphPinRequestDataNewAPI[numberOfPins];

        for (int i = 0; i < numberOfPins; i++) {
            requestedPins[i] = new GraphPinRequestDataNewAPI(dashId, messageParts, i, msgId, valuesPerPin);
        }

        return reportingDao.getAllFromDisk(user.name, requestedPins);
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

    private class GraphPinRequestData extends GraphPinRequest {

        public GraphPinRequestData(String[] messageParts, final int pinIndex, int msgId, int valuesPerPin) {
            try {
                dashId = Integer.parseInt(messageParts[pinIndex * valuesPerPin]);
                pinType = PinType.getPingType(messageParts[pinIndex * valuesPerPin + 1].charAt(0));
                pin = Byte.parseByte(messageParts[pinIndex * valuesPerPin + 2]);
                count = Integer.parseInt(messageParts[pinIndex * valuesPerPin + 3]);
                type = GraphType.getPeriodByType(messageParts[pinIndex * valuesPerPin + 4].charAt(0));
            } catch (NumberFormatException e) {
                throw new IllegalCommandException("HardwareLogic command body incorrect.", msgId);
            }
        }
    }

    private class GraphPinRequestDataNewAPI extends GraphPinRequest{

          public GraphPinRequestDataNewAPI(int dashId, String[] messageParts, final int pinIndex, int msgId, int valuesPerPin) {
            try {
                this.dashId = dashId;
                pinType = PinType.getPingType(messageParts[pinIndex * valuesPerPin ].charAt(0));
                pin = Byte.parseByte(messageParts[pinIndex * valuesPerPin + 1]);
                count = Integer.parseInt(messageParts[pinIndex * valuesPerPin + 2]);
                type = GraphType.getPeriodByType(messageParts[pinIndex * valuesPerPin + 3].charAt(0));
            } catch (NumberFormatException e) {
                throw new IllegalCommandException("HardwareLogic command body incorrect.", msgId);
            }
        }
    }

    public abstract class GraphPinRequest {

        public int dashId;

        public PinType pinType;

        public byte pin;

        public int count;

        public GraphType type;

    }

}

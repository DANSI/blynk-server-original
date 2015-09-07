package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.appllication.GetGraphDataResponseMessage;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoDataException;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.enums.GraphType;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private final StorageDao storageDao;

    public GetGraphDataLogic(StorageDao storageDao) {
        this.storageDao = storageDao;
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
            if (deleteGraphData(messageParts, user.getName(), message.id)) {
                ctx.writeAndFlush(produce(message.id, OK));
            }
        } else {
            byte[][] data = process(messageParts, user, message.id);

            if (checkNoData(data)) {
                throw new NoDataException(message.id);
            }

            log.trace("Sending getGraph response. ");
            byte[] compressed = compress(data, message.id);
            ctx.writeAndFlush(new GetGraphDataResponseMessage(message.id, compressed));
        }
    }

    private byte[][] process(String[] messageParts, User user, int msgId) {
        int numberOfPins = messageParts.length / VALUES_PER_PIN;

        GraphPinRequestData[] requestedPins = new GraphPinRequestData[numberOfPins];

        for (int i = 0; i < numberOfPins; i++) {
            requestedPins[i] = new GraphPinRequestData(messageParts, i, msgId);
            user.getProfile().validateDashId(requestedPins[i].dashBoardId, msgId);
        }

        byte[][] values = new byte[numberOfPins][];

        for (int i = 0; i < numberOfPins; i++) {
            values[i] = storageDao.getAllFromDisk(user.getName(),
                    requestedPins[i].dashBoardId, requestedPins[i].pinType,
                    requestedPins[i].pin, requestedPins[i].count, requestedPins[i].type);
        }

        return values;
    }

    private boolean deleteGraphData(String[] messageParts, String username, int msgId) {
        try {
            int dashBoardId = Integer.parseInt(messageParts[0]);
            PinType pinType = PinType.getPingType(messageParts[1].charAt(0));
            byte pin = Byte.parseByte(messageParts[2]);
            String cmd = messageParts[3];
            if ("del".equals(cmd)) {
                storageDao.delete(username, dashBoardId, pinType, pin);
                return true;
            }
        } catch (NumberFormatException e) {
            throw new IllegalCommandException("HardwareLogic command body incorrect.", msgId);
        }
        return false;
    }

    private class GraphPinRequestData {

        int dashBoardId;

        PinType pinType;

        byte pin;

        int count;

        GraphType type;

        public GraphPinRequestData(String[] messageParts, final int pinIndex, int msgId) {
            try {
                dashBoardId = Integer.parseInt(messageParts[pinIndex * VALUES_PER_PIN]);
                pinType = PinType.getPingType(messageParts[pinIndex * VALUES_PER_PIN + 1].charAt(0));
                pin = Byte.parseByte(messageParts[pinIndex * VALUES_PER_PIN + 2]);
                count = Integer.parseInt(messageParts[pinIndex * VALUES_PER_PIN + 3]);
                type = GraphType.getPeriodByType(messageParts[pinIndex * VALUES_PER_PIN + 4].charAt(0));
            } catch (NumberFormatException e) {
                throw new IllegalCommandException("HardwareLogic command body incorrect.", msgId);
            }
        }
    }

}

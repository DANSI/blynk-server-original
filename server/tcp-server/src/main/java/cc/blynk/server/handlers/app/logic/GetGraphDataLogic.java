package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.appllication.GetGraphDataResponseMessage;
import cc.blynk.server.exceptions.GetGraphDataException;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoDataException;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.enums.GraphType;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;

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

    public static byte[] compress(byte[][] values, int msgId) {
        //todo calculate size
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            for (byte[] data : values) {
                ByteBuffer bb = ByteBuffer.allocate(4);
                bb.putInt(data.length / 16);
                out.write(bb.array());
                out.write(data);
            }
        } catch (Exception ioe) {
            throw new GetGraphDataException(msgId);
        }
        return baos.toByteArray();
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        //warn: split may be optimized
        String[] messageParts = message.body.split(" ");

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        int numberOfPins = messageParts.length / VALUES_PER_PIN;

        GraphPinRequestData[] requestedPins = new GraphPinRequestData[numberOfPins];

        for (int i = 0; i < numberOfPins; i++) {
            try {
                requestedPins[i] = new GraphPinRequestData(messageParts, i);
                user.getProfile().validateDashId(requestedPins[i].dashBoardId, message.id);
            } catch (NumberFormatException e) {
                throw new IllegalCommandException("HardwareLogic command body incorrect.", message.id);
            }
        }

        boolean noData = true;
        byte[][] values = new byte[numberOfPins][];

        for (int i = 0; i < numberOfPins; i++) {
            values[i] = storageDao.getAllFromDisk(user.getName(),
                    requestedPins[i].dashBoardId, requestedPins[i].pinType,
                    requestedPins[i].pin, requestedPins[i].count, requestedPins[i].type);

            noData = noData && values[i].length == 0;
        }

        if (noData) {
            throw new NoDataException(message.id);
        }

        byte[] compressed = compress(values, message.id);

        log.trace("Sending getGraph response. ");

        ctx.writeAndFlush(new GetGraphDataResponseMessage(message.id, compressed));
    }

    private class GraphPinRequestData {

        int dashBoardId;

        PinType pinType;

        byte pin;

        int count;

        GraphType type;

        public GraphPinRequestData(String[] messageParts, final int pinIndex) {
            dashBoardId = Integer.parseInt(messageParts[pinIndex * VALUES_PER_PIN]);
            pinType = PinType.getPingType(messageParts[pinIndex * VALUES_PER_PIN + 1].charAt(0));
            pin = Byte.parseByte(messageParts[pinIndex * VALUES_PER_PIN + 2]);
            count = Integer.parseInt(messageParts[pinIndex * VALUES_PER_PIN + 3]);
            type = GraphType.getPeriodByType(messageParts[pinIndex * VALUES_PER_PIN + 4].charAt(0));
        }
    }

}

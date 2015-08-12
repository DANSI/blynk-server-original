package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.appllication.GetGraphDataResponseMessage;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.graph.GraphKey;
import cc.blynk.server.dao.graph.StoreMessage;
import cc.blynk.server.exceptions.GetGraphDataException;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.zip.DeflaterOutputStream;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetGraphDataLogic {

    private final static byte[] EMPTY_RESPONSE = {};
    private final StorageDao storageDao;

    public GetGraphDataLogic(StorageDao storageDao) {
        this.storageDao = storageDao;
    }

    public static byte[] compress(Queue<StoreMessage> values, int msgId) {
        if (values == null || values.size() == 0) {
            return EMPTY_RESPONSE;
        }
        //todo define size
        ByteArrayOutputStream baos = new ByteArrayOutputStream(values.size() * 20);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            int counter = 0;
            int length = values.size();
            for (StoreMessage s : values) {
                counter++;
                out.write(s.toString().getBytes());
                if (counter < length) {
                    out.write(StringUtils.BODY_SEPARATOR);
                }
            }
        } catch (Exception ioe) {
            throw new GetGraphDataException(msgId);
        }
        return baos.toByteArray();
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
        //warn: split may be optimized
        String[] messageParts = message.body.split(" ", 3);

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        String dashBoardIdString = messageParts[0];
        String pinTypeChar = messageParts[1];
        String pinString = messageParts[2];

        int dashBoardId;
        PinType pinType;
        byte pin;

        try {
            dashBoardId = Integer.parseInt(dashBoardIdString);
            pinType = PinType.getPingType(pinTypeChar.charAt(0));
            pin = Byte.parseByte(pinString);
        } catch (NumberFormatException e) {
            throw new IllegalCommandException("HardwareLogic command body incorrect.", message.id);
        }

        user.getProfile().validateDashId(dashBoardId, message.id);

        Queue<StoreMessage> allValues = storageDao.getAllFromMemmory(new GraphKey(dashBoardId, pin, pinType));

        byte[] compressed = compress(allValues, message.id);

        ctx.writeAndFlush(new GetGraphDataResponseMessage(message.id, compressed));
    }

}

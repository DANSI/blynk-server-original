package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.appllication.GetGraphDataResponseMessage;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.exceptions.GetGraphDataException;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NoDataException;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.enums.PinType;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.zip.DeflaterOutputStream;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetGraphDataLogic {

    private final StorageDao storageDao;

    public GetGraphDataLogic(StorageDao storageDao) {
        this.storageDao = storageDao;
    }

    public static byte[] compress(Collection<?> values, int msgId) {
        //todo define size
        ByteArrayOutputStream baos = new ByteArrayOutputStream(values.size() * 20);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            int counter = 0;
            int length = values.size();
            for (Object s : values) {
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
        String[] messageParts = message.body.split(" ");

        if (messageParts.length < 3) {
            throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        int dashBoardId;
        PinType pinType;
        byte pin;
        int period = 0;

        try {
            dashBoardId = Integer.parseInt(messageParts[0]);
            pinType = PinType.getPingType(messageParts[1].charAt(0));
            pin = Byte.parseByte(messageParts[2]);
            if (messageParts.length == 4) {
                period = Integer.parseInt(messageParts[3]);
            }

        } catch (NumberFormatException e) {
            throw new IllegalCommandException("HardwareLogic command body incorrect.", message.id);
        }

        user.getProfile().validateDashId(dashBoardId, message.id);

        byte[] compressed;

        Collection<?> values;
        if (period == 0) {
            values = storageDao.getAllFromMemmory(dashBoardId, pinType, pin);
        } else {
            values = storageDao.getAllFromDisk(user.getName(), dashBoardId, pinType, pin, period);

        }

        if (values == null || values.size() == 0) {
            throw new NoDataException(message.id);
        }

        compressed = compress(values, message.id);

        ctx.writeAndFlush(new GetGraphDataResponseMessage(message.id, compressed));
    }

}

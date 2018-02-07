package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.Purchase;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.03.16.
 */
public class AddEnergyLogic {

    private static final Logger log = LogManager.getLogger(AddEnergyLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;

    public AddEnergyLogic(DBManager dbManager, BlockingIOProcessor blockingIOProcessor) {
        this.blockingIOProcessor = blockingIOProcessor;
        this.dbManager = dbManager;
    }

    private static boolean isValidTransactionId(String id) {
        if (id == null || id.isEmpty() || id.startsWith("com.blynk.energy")) {
            return false;
        }

        if (id.length() == 36) {
            // fake example - "8077004819764738793.5939465896020147"
            String[] transactionParts = id.split("\\.");
            if (transactionParts.length == 2
                    && transactionParts[0].length() == 19
                    && transactionParts[1].length() == 16) {
                return false;
            }
        }

        return true;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] bodyParts = split2(message.body);

        int energyAmountToAdd = Integer.parseInt(bodyParts[0]);
        ResponseMessage response;
        if (bodyParts.length == 2 && isValidTransactionId(bodyParts[1])) {
            insertPurchase(user.email, energyAmountToAdd, bodyParts[1]);
            user.addEnergy(energyAmountToAdd);
            response = ok(message.id);
        } else {
            log.debug("Purchase with invalid transaction id '{}'. {}.", message.body, user.email);
            response = notAllowed(message.id);
        }
        ctx.writeAndFlush(response, ctx.voidPromise());
    }

    private void insertPurchase(String email, int reward, String transactionId) {
        if (transactionId.equals("AdColonyAward") || transactionId.equals("homeScreen")) {
            return;
        }
        blockingIOProcessor.executeDB(
            () -> dbManager.insertPurchase(new Purchase(email, reward, transactionId))
        );
    }

}

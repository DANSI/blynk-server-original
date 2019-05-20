package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.BlockingIOProcessor;
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
public class MobilePurchaseLogic {

    private static final Logger log = LogManager.getLogger(MobilePurchaseLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;
    private boolean wasErrorPrinted;

    public MobilePurchaseLogic(Holder holder) {
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.dbManager = holder.dbManager;
        this.wasErrorPrinted = false;
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

    private static double calcPrice(int reward) {
        switch (reward) {
            case 200 :
                return 0D;
            case 1000 :
                return 0.99D;
            case 2400 :
                return 1.99D;
            case 5000 :
                return 3.99D;
            case 13000 :
                return 9.99D;
            case 28000 :
                return 19.99D;
            default:
                return -1D;
        }
    }

    public void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state, StringMessage message) {
        var splitBody = split2(message.body);
        var user = state.user;

        var energyAmountToAdd = Integer.parseInt(splitBody[0]);
        ResponseMessage response;
        if (splitBody.length == 2 && isValidTransactionId(splitBody[1])) {
            double price = calcPrice(energyAmountToAdd);
            insertPurchase(user.email, energyAmountToAdd, price, splitBody[1]);
            user.addEnergy(energyAmountToAdd);
            response = ok(message.id);
        } else {
            if (!wasErrorPrinted) {
                log.warn("Purchase {} with invalid transaction id '{}'. {} ({}).",
                        splitBody[0], splitBody[1], user.email, state.version);
                wasErrorPrinted = true;
            }
            response = notAllowed(message.id);
        }
        ctx.writeAndFlush(response, ctx.voidPromise());
    }

    private void insertPurchase(String email, int reward, double price, String transactionId) {
        if (transactionId.equals("AdColonyAward") || transactionId.equals("homeScreen")) {
            return;
        }
        blockingIOProcessor.executeDB(
            () -> dbManager.insertPurchase(new Purchase(email, reward, price, transactionId))
        );
    }

}

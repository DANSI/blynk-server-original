package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
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
public class AddEnergyLogic {

    private static final Logger log = LogManager.getLogger(AddEnergyLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;
    private boolean wasErrorPrinted;

    public AddEnergyLogic(DBManager dbManager, BlockingIOProcessor blockingIOProcessor) {
        this.blockingIOProcessor = blockingIOProcessor;
        this.dbManager = dbManager;
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

            // fake example "51944AFD-1D24-4A22-A51F-93513A76CD28"
            transactionParts = id.split("-");
            if (transactionParts.length == 5 && transactionParts[0].length() == 8
                    && transactionParts[1].length() == 4
                    && transactionParts[2].length() == 4
                    && transactionParts[3].length() == 4
                    && transactionParts[4].length() == 12) {
                return false;
            }
        }

        return true;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        var splitBody = split2(message.body);
        var user = state.user;

        var energyAmountToAdd = Integer.parseInt(splitBody[0]);
        ResponseMessage response;
        if (splitBody.length == 2 && isValidTransactionId(splitBody[1])) {
            insertPurchase(user.email, energyAmountToAdd, splitBody[1]);
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

    private void insertPurchase(String email, int reward, String transactionId) {
        if (transactionId.equals("AdColonyAward") || transactionId.equals("homeScreen")) {
            return;
        }
        blockingIOProcessor.executeDB(
            () -> dbManager.insertPurchase(new Purchase(email, reward, transactionId))
        );
    }

}

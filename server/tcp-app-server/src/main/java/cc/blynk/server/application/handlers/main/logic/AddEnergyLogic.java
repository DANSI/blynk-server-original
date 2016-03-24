package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.Purchase;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.ByteBufUtil.*;


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

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] bodyParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING, 2);

        int energyAmountToAdd = ParseUtil.parseInt(bodyParts[0], message.id);
        if (bodyParts.length == 2) {
            final String transactionId = bodyParts[1];
            insertPurchase(user.name, energyAmountToAdd, transactionId);
        } else {
            log.error("Purchase without transaction id. User {}. Reward {}", user.name, energyAmountToAdd);
        }

        user.purchaseEnergy(energyAmountToAdd);
        ctx.writeAndFlush(ok(ctx, message.id), ctx.voidPromise());
    }

    private void insertPurchase(String username, int reward, String transactionId) {
        blockingIOProcessor.execute(
                () -> dbManager.insertPurchase(new Purchase(username, reward, transactionId))
        );
    }

}

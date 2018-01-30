package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.Redeem;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * Handler responsible for handling redeem logic. Unlocks premium content for predefined tokens.
 * Used for kickstarter backers and other companies that paid for redeeming.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.03.16.
 */
public class RedeemLogic {

    private static final Logger log = LogManager.getLogger(RedeemLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;

    public RedeemLogic(DBManager dbManager, BlockingIOProcessor blockingIOProcessor) {
        this.blockingIOProcessor = blockingIOProcessor;
        this.dbManager = dbManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String redeemToken = message.body;

        blockingIOProcessor.executeDB(() ->
                ctx.writeAndFlush(verifyToken(message, redeemToken, user), ctx.voidPromise()));
    }

    private MessageBase verifyToken(StringMessage message, String redeemToken, User user) {
        try {
            Redeem redeem = dbManager.selectRedeemByToken(redeemToken);
            if (redeem != null) {
                if (redeem.isRedeemed && redeem.email.equals(user.email)) {
                    return ok(message.id);
                } else if (!redeem.isRedeemed && dbManager.updateRedeem(user.email, redeemToken)) {
                    unlockContent(user, redeem.reward);
                    return ok(message.id);
                }
            }
        } catch (Exception e) {
            log.debug("Error redeeming token.", e);
        }

        return notAllowed(message.id);
    }

    private void unlockContent(User user, int reward) {
        user.addEnergy(reward);
        log.info("Unlocking content for {}. Reward {}.", user.email, reward);
    }

}

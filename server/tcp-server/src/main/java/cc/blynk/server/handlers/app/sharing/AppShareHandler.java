package cc.blynk.server.handlers.app.sharing;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.main.logic.LoadProfileLogic;
import cc.blynk.server.handlers.app.main.logic.reporting.GetGraphDataLogic;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.ThreadContext;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AppShareHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final AppShareStateHolder state;
    private final HardwareAppShareLogic hardwareApp;
    private final GetGraphDataLogic graphData;

    public AppShareHandler(ServerProperties props, UserDao userDao, SessionDao sessionDao, ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor, AppShareStateHolder state) {
        super(props);
        this.hardwareApp = new HardwareAppShareLogic(sessionDao);
        this.graphData = new GetGraphDataLogic(reportingDao, blockingIOProcessor);
        this.state = state;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        ThreadContext.put("user", state.user.name);
        switch (msg.command) {
            case HARDWARE:
                hardwareApp.messageReceived(ctx, state, msg);
                break;
            case LOAD_PROFILE :
                LoadProfileLogic.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
        }
    }

}

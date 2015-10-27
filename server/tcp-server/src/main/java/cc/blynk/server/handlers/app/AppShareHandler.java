package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.auth.AppStateHolder;
import cc.blynk.server.handlers.app.logic.HardwareAppLogic;
import cc.blynk.server.handlers.app.logic.LoadProfileGzippedLogic;
import cc.blynk.server.handlers.app.logic.LoadProfileLogic;
import cc.blynk.server.handlers.app.logic.reporting.GetGraphDataLogic;
import cc.blynk.server.handlers.app.logic.sharing.SyncLogic;
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

    public final AppStateHolder state;
    private final HardwareAppLogic hardwareApp;
    private final GetGraphDataLogic graphData;
    private final SyncLogic syncLogic;

    public AppShareHandler(ServerProperties props, UserDao userDao, SessionDao sessionDao, ReportingDao reportingDao, BlockingIOProcessor blockingIOProcessor, AppStateHolder state) {
        super(props);
        this.hardwareApp = new HardwareAppLogic(sessionDao);
        this.graphData = new GetGraphDataLogic(reportingDao, blockingIOProcessor);
        this.syncLogic = new SyncLogic();
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
            case GET_GRAPH_DATA :
                graphData.messageReceived(ctx, state.user, msg);
                break;
            case LOAD_PROFILE_GZIPPED :
                LoadProfileGzippedLogic.messageReceived(ctx, state.user, msg);
                break;
            case SYNC :
                syncLogic.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
            //todo remove for release
            default: throw new RuntimeException("Unexpected message");
        }
    }

}

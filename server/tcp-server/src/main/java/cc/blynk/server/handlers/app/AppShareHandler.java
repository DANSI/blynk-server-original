package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.app.logic.GetGraphDataLogic;
import cc.blynk.server.handlers.app.logic.HardwareAppLogic;
import cc.blynk.server.handlers.app.logic.LoadProfileLogic;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AppShareHandler extends BaseSimpleChannelInboundHandler<Message> {

    private final HardwareAppLogic hardwareApp;
    private final GetGraphDataLogic graphData;

    public AppShareHandler(ServerProperties props, UserDao userDao, SessionDao sessionDao, ReportingDao reportingDao, HandlerState state) {
        super(props, state);
        this.hardwareApp = new HardwareAppLogic(sessionDao);
        this.graphData = new GetGraphDataLogic(reportingDao);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HandlerState state, Message msg) {
        switch (msg.command) {
            case HARDWARE:
                hardwareApp.messageReceived(ctx, state.user, msg);
                break;
            case LOAD_PROFILE :
                LoadProfileLogic.messageReceived(ctx, state.user, msg);
                break;
            case GET_GRAPH_DATA :
                graphData.messageReceived(ctx, state.user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
            //todo remove for release
            default: throw new RuntimeException("Unexpected message");
        }
    }

}

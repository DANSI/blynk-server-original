package cc.blynk.server.api.http.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.http.rest.Response;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cc.blynk.server.handlers.http.rest.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.12.15.
 */
@Path("/data")
public class HttpBusinessAPILogic {

    private static final Logger log = LogManager.getLogger(HttpBusinessAPILogic.class);

    private final UserDao userDao;
    private final BlockingIOProcessor blockingIOProcessor;
    private final SessionDao sessionDao;
    private final GlobalStats globalStats;
    private final MailWrapper mailWrapper;
    private final GCMWrapper gcmWrapper;
    private final ReportingDao reportingDao;

    public HttpBusinessAPILogic(Holder holder) {
        this(holder.userDao, holder.sessionDao, holder.blockingIOProcessor, holder.mailWrapper, holder.gcmWrapper, holder.reportingDao, holder.stats);
    }

    private HttpBusinessAPILogic(UserDao userDao, SessionDao sessionDao, BlockingIOProcessor blockingIOProcessor,
                                 MailWrapper mailWrapper, GCMWrapper gcmWrapper, ReportingDao reportingDao, GlobalStats globalStats) {
        this.userDao = userDao;
        this.blockingIOProcessor = blockingIOProcessor;
        this.sessionDao = sessionDao;
        this.globalStats = globalStats;
        this.mailWrapper = mailWrapper;
        this.gcmWrapper = gcmWrapper;
        this.reportingDao = reportingDao;
    }

    @GET
    @Path("")
    public Response getDashboard(@QueryParam("name") String name,
                                 @QueryParam("groupBy") String groupBy,
                                 @QueryParam("aggregation") String aggregation,
                                 @QueryParam("pin") String pin,
                                 @QueryParam("value") String value) {
        User user = userDao.getByName("parking@gmail.com");
        List<DashBoard> projects = new ArrayList<>(Arrays.asList(user.profile.dashBoards));

        projects = filterByProjectName(projects, name);
        projects = filterByValue(projects, pin, value);

        if (groupBy == null || aggregation == null) {
            return ok(projects);
        }


        Map<String, Long> result = groupBy(projects, groupBy, aggregation);

        return ok(result);
    }

    private static Map<String, Long> groupBy(List<DashBoard> projects, String groupBy, String aggregation) {
        Map<String, Long> countByGroupBy = projects.stream()
                .collect(Collectors.groupingBy(DashBoard::getName, Collectors.counting()));

        return countByGroupBy;
    }



    private static List<DashBoard> filterByValue(List<DashBoard> projects, String pin, String value) {
        if (value == null) {
            return projects;
        }

        PinType pinType = PinType.getPinType(pin.charAt(0));
        byte pinIndex = Byte.parseByte(pin.substring(1));

        return projects.stream().filter(
                project -> {
                    Widget widget = project.findWidgetByPin(pinIndex, pinType);
                    String widgetValue = widget.getValue(pinIndex, pinType);
                    return value.equalsIgnoreCase(widgetValue);
                }
        ).collect(Collectors.toList());
    }

    private static List<DashBoard> filterByProjectName(List<DashBoard> projects, String name) {
        if (name == null) {
            return projects;
        }
        return projects.stream().filter(
                project -> name.equalsIgnoreCase(project.name)
        ).collect(Collectors.toList());
    }

}

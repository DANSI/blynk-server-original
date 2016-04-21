package cc.blynk.server.api.http.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.api.http.pojo.business.BusinessProject;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.handlers.http.rest.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
@Path("/")
public class HttpBusinessAPILogic {

    private static final Logger log = LogManager.getLogger(HttpBusinessAPILogic.class);

    private final UserDao userDao;

    public HttpBusinessAPILogic(Holder holder) {
        this(holder.userDao);
    }

    private HttpBusinessAPILogic(UserDao userDao) {
        this.userDao = userDao;
    }

    @GET
    @Path("{token}/query")
    public Response getDashboard(@PathParam("token") String token,
                                 @QueryParam("name") String name,
                                 @QueryParam("groupBy") String groupBy,
                                 @QueryParam("aggregation") String aggregation,
                                 @QueryParam("pin") String pin,
                                 @QueryParam("value") String value) {

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        List<DashBoard> projects = new ArrayList<>(Arrays.asList(user.profile.dashBoards));

        projects = filterByProjectName(projects, name);
        projects = filterByValue(projects, pin, value);

        if (groupBy == null || aggregation == null) {
            return ok(transform(projects));
        }


        Map<String, Long> result = groupBy(projects, groupBy, aggregation);

        return ok(result);
    }

    private static List<BusinessProject> transform(List<DashBoard> projects) {
        List<BusinessProject> businessProjects = new ArrayList<>();
        for (DashBoard dashBoard : projects) {
            businessProjects.add(new BusinessProject(dashBoard));
        }
        return businessProjects;
    }

    //todo finish
    private static Map<String, Long> groupBy(List<DashBoard> projects, String groupBy, String aggregation) {
        return projects.stream().collect(Collectors.groupingBy(DashBoard::getName, Collectors.counting()));
    }

    private static List<DashBoard> filterByValue(List<DashBoard> projects, String pin, String value) {
        if (value == null) {
            return projects;
        }

        if (pin != null) {
            return filterByValueAndPin(projects, pin, value);
        }

        return filterByValue(projects, value);
    }

    private static List<DashBoard> filterByValueAndPin(List<DashBoard> projects, String pin, String value) {
        PinType pinType = PinType.getPinType(pin.charAt(0));
        byte pinIndex = Byte.parseByte(pin.substring(1));

        return projects.stream().filter(
                project -> {
                    Widget widget = project.findWidgetByPin(pinIndex, pinType);
                    if (widget == null) {
                        return false;
                    }
                    String widgetValue = widget.getValue(pinIndex, pinType);
                    return value.equalsIgnoreCase(widgetValue);
                }
        ).collect(Collectors.toList());
    }

    private static List<DashBoard> filterByValue(List<DashBoard> projects, String value) {
        return projects.stream().filter(
                project -> {
                    for (Widget widget : project.widgets) {
                        if (widget.hasValue(value)) {
                            return true;
                        }
                    }
                    return false;
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

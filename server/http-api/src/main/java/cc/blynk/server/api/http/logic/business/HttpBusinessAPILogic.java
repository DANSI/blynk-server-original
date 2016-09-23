package cc.blynk.server.api.http.logic.business;

import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.PathParam;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.pojo.business.BusinessProject;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static cc.blynk.core.http.Response.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.12.15.
 */
@Path("/")
@SuppressWarnings("unchecked")
public class HttpBusinessAPILogic {

    private static final Logger log = LogManager.getLogger(HttpBusinessAPILogic.class);

    private final UserDao userDao;

    public HttpBusinessAPILogic(Holder holder) {
        this(holder.userDao);
    }

    private HttpBusinessAPILogic(UserDao userDao) {
        this.userDao = userDao;
    }

    private static List<Map> transform(Map<Map, Long> groupingResult, String aggregation) {
        List<Map> result = new ArrayList<>(groupingResult.size());

        for (Map.Entry<Map, Long> entry : groupingResult.entrySet()) {
            Map<String, Object> key = entry.getKey();
            key.put(aggregation, entry.getValue());
            result.add(key);
        }

        return result;
    }

    /**
     * Simplifies output object.
     */
    private static List<BusinessProject> transform(List<DashBoard> projects) {
        return projects.stream().map(BusinessProject::new).collect(Collectors.toList());
    }

    private static Map<Map, Long> groupBy(List<DashBoard> projects, List<String> groupByList, String aggregation) {
        return projects.stream().collect(Collectors.groupingBy(
                        proj -> {
                            Map<String, Object> result = new HashMap<>();
                            for (String groupBy : groupByList) {
                                switch (groupBy) {
                                    case "name":
                                        result.put(groupBy, proj.name);
                                        break;
                                    default:
                                        result.put(groupBy, proj.metadata.get(groupBy));
                                        break;
                                }
                            }

                            return result;
                        },
                        getAggregation(aggregation)
                )
        );
    }

    private static <T> Collector<T, ?, Long> getAggregation(String aggregation) {
        switch (aggregation) {
            case "count" :
                return Collectors.counting();
            default:
                throw new RuntimeException("Not supported aggregation function " + aggregation);
        }
    }

    private static Predicate<DashBoard> filterByValue(String pin, String value) {
        if (value == null) {
            return x -> true;
        }

        if (pin != null) {
            return filterByValueAndPin(pin, value);
        }

        return filterByValue(value);
    }

    private static Predicate<DashBoard> filterByValueAndPin(String pin, String value) {
        PinType pinType = PinType.getPinType(pin.charAt(0));
        byte pinIndex = Byte.parseByte(pin.substring(1));

        return
                project -> {
                    Widget widget = project.findWidgetByPin(pinIndex, pinType);
                    if (widget == null) {
                        return false;
                    }
                    String widgetValue = widget.getValue(pinIndex, pinType);
                    return value.equalsIgnoreCase(widgetValue);
                };
    }

    private static Predicate<DashBoard> filterByValue(String value) {
        return
                project -> {
                    for (Widget widget : project.widgets) {
                        if (widget.hasValue(value)) {
                            return true;
                        }
                    }
                    return false;
                };
    }

    private static Predicate<DashBoard> filterByProjectName(String name) {
        return project -> name == null || name.equalsIgnoreCase(project.name);
    }

    @GET
    @Path("{token}/query")
    public Response getDashboard(@PathParam("token") String token,
                                 @QueryParam("name") String name,
                                 @QueryParam("groupBy") List<String> groupByList,
                                 @QueryParam("aggregation") String aggregation,
                                 @QueryParam("pin") String pin,
                                 @QueryParam("value") String value) {

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.error("Requested token {} not found.", token);
            return Response.badRequest("Invalid token.");
        }

        List<DashBoard> projects = Arrays.asList(user.profile.dashBoards).stream()
            .filter(filterByProjectName(name))
            .filter(filterByValue(pin, value))
                .collect(Collectors.toList());

        if (groupByList == null || aggregation == null) {
            return ok(transform(projects));
        }

        Map<Map, Long> groupingResult = groupBy(projects, groupByList, aggregation);

        return ok(transform(groupingResult, aggregation));
    }

}

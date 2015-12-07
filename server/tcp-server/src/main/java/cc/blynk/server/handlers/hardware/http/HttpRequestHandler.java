package cc.blynk.server.handlers.hardware.http;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.StatsWorker;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

import java.util.*;
import java.util.stream.Collectors;

import static cc.blynk.server.handlers.hardware.http.ResponseGenerator.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.12.15.
 */
public class HttpRequestHandler {

    private final GlobalStats stats;
    private final SessionDao sessionDao;
    private final UserDao userDao;

    public HttpRequestHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        this.stats = globalStats;
    }

    private static List<Pair> sort(List<Pair> list, String field, String order, boolean nameAsInt) {
        Comparator<Pair> c = "name".equals(field) ? (nameAsInt ? Pair.byNameAsInt : Pair.byName) : Pair.byCount;
        Collections.sort(list, "ASC".equals(order) ? c : Collections.reverseOrder(c));
        return list;
    }

    private static List<Pair> sort(List<Pair> list, String field, String order) {
        return sort(list, field, order, false);
    }

    private static List<Pair> convertMapToPair(Map<String, Integer> map) {
        return map.entrySet().stream().map(Pair::new).collect(Collectors.toList());
    }

    public FullHttpResponse processRequest(HttpRequest req, URIDecoder uriDecoder) {
        switch (req.getMethod().name()) {
            case "GET" :
                return GET(uriDecoder);
            default :
                return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
        }
    }

    private FullHttpResponse GET(URIDecoder uriDecoder) {
        switch (uriDecoder.getEntity()) {
            case "users" :
                if (uriDecoder.hasId()) {
                    return makeResponse(userDao.getUsers().get(uriDecoder.getId()));
                } else {
                    Collection<User> users = userDao.saerchByUsername(uriDecoder.getNameFilter());
                    return appendTotalCountHeader(
                            makeResponse(users, uriDecoder.getPage(), uriDecoder.getPageSize()), users.size()
                    );
                }

            case "stats" :
                switch (uriDecoder.getSubEntity()) {
                    case "realtime" :
                        return makeResponse(Collections.singletonList(StatsWorker.calcStats(sessionDao, userDao, stats, false)));
                    case "boards" :
                        return makeResponse(sort(convertMapToPair(userDao.getBoardsUsage()), uriDecoder.getSortField(), uriDecoder.getSortOrder()));
                    case "widgets" :
                        return makeResponse(sort(convertMapToPair(userDao.getWidgetsUsage()), uriDecoder.getSortField(), uriDecoder.getSortOrder()));
                    case "projectsperuser" :
                        return makeResponse(sort(convertMapToPair(userDao.getProjectsPerUser()), uriDecoder.getSortField(), uriDecoder.getSortOrder(), true));
                    case "filledspace" :
                        return makeResponse(sort(convertMapToPair(userDao.getFilledSpace()), uriDecoder.getSortField(), uriDecoder.getSortOrder(), true));
                }
        }

        return null;
    }

}

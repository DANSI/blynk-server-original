package cc.blynk.server.core.administration.actions;

import cc.blynk.server.core.administration.Executable;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class used in order to monitor current users activity.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.04.15.
 */
public class ActivityMonitor implements Executable {

    private static final Logger log = LogManager.getLogger(ActivityMonitor.class);

    private static final Comparator<HardwareHandler> quotaComparator = (o1, o2) ->
            Double.compare(o2.getQuotaMeter().getOneMinuteRateNoTick(), o1.getQuotaMeter().getOneMinuteRateNoTick());

    @Override
    public List<String> execute(UserDao userDao, SessionDao sessionDao, String... params) {
        List<String> result = new ArrayList<>();

        result.add("Registry size : " + userDao.getUsers().size() + "\n");

        //todo change code
        /*
        List<User> usersByRate = new ArrayList<>(userDao.getUsers().values());

        Collections.sort(usersByRate, quotaComparator);

        for (User user : usersByRate) {
            String val = user.getName() + " - " + user.getQuotaMeter().getOneMinuteRate() + "\n";
            log.info(val);
            result.add(val);
        }
        */

        log.info("ok");
        result.add("ok\n");
        return result;
    }

}

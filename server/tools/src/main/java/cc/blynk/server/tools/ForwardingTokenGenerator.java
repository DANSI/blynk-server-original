package cc.blynk.server.tools;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.dao.ForwardingTokenEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 17.11.17.
 */
public final class ForwardingTokenGenerator {

    private ForwardingTokenGenerator() {

    }

    public static void main(String[] args) throws Exception {
        BlockingIOProcessor blockingIOProcessor = new BlockingIOProcessor(6, 100);

        try (DBManager dbManager = new DBManager(blockingIOProcessor, true)) {
            process(dbManager, "singapore", "188.166.206.43");
            process(dbManager, "frankfurt", "139.59.206.133");
            process(dbManager, "ny3", "45.55.96.146");
        }
    }

    private static void process(DBManager dbManager, String region, String ip) throws Exception {
        System.out.println("Reading all users from region : " + region + ". Forward to " + ip);
        ConcurrentMap<UserKey, User> users = dbManager.userDBDao.getAllUsers(region);
        System.out.println("Read " + users.size() + " users.");
        int count = 0;
        List<ForwardingTokenEntry> entryList = new ArrayList<>(1100);
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Device device : dashBoard.devices) {
                    if (device != null && device.token != null) {
                        count++;
                        entryList.add(new ForwardingTokenEntry(device.token, ip, user.email, dashBoard.id, device.id));
                    }
                }
            }
            if (entryList.size() > 1000) {
                dbManager.forwardingTokenDBDao.insertTokenHostBatch(entryList);
                System.out.println(entryList.size() + " tokens inserted.");
                entryList = new ArrayList<>(1100);
            }
        }

        if (entryList.size() > 0) {
            dbManager.forwardingTokenDBDao.insertTokenHostBatch(entryList);
            System.out.println(entryList.size() + " tokens inserted.");
        }

        System.out.println("Total entries : " + count);
    }

}

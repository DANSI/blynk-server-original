package cc.blynk.integration.tools;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.db.DBManager;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;

public class UserReader {

    public static void main(String[] args) throws Exception {
        DBManager dbManager = new DBManager("db-test.properties", new BlockingIOProcessor(1, 100), true);
        ConcurrentMap<UserKey, User> allUsers = dbManager.userDBDao.getAllUsers("");
        System.out.println("Users : " + allUsers.size());
        for (User user : allUsers.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Device device : dashBoard.devices) {
                    System.out.println(user.email + "," + device.getNameOrDefault()
                                               + "," + new Date(device.firstConnectTime)
                                               + "," + device.token);
                }
            }
        }
    }
}

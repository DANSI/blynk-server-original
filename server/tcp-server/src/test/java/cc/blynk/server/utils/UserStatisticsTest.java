package cc.blynk.server.utils;

import cc.blynk.server.TestBase;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.Widget;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ddumanskiy
 * Date: 09.12.13
 * Time: 8:07
 */
@Ignore
public class UserStatisticsTest extends TestBase {

    static FileManager fileManager;
    static Map<String, User> users;

    @BeforeClass
    public static void init() {
        fileManager = new FileManager("/home/doom369/test/root/data");
        users = fileManager.deserialize();

        System.out.println(users.size());
    }

    @Test
    public void printBoardTypes() {
        System.out.println();
        System.out.println("Board Types :");
        Map<String, Integer> boards = new HashMap<>();
        for (User user : users.values()) {
            if (user.getProfile().getDashBoards() != null) {
                for (DashBoard dashBoard : user.getProfile().getDashBoards()) {
                    String type = dashBoard.getBoardType();
                    Integer i = boards.get(type);
                    if (i == null) {
                        i = 0;
                    }
                    boards.put(type, ++i);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : boards.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    @Test
    public void printWidgetUsage() {
        System.out.println();
        System.out.println("Widget Usage :");
        Map<String, Integer> boards = new HashMap<>();
        for (User user : users.values()) {
            if (user.getProfile().getDashBoards() != null) {
                for (DashBoard dashBoard : user.getProfile().getDashBoards()) {
                    if (dashBoard.getWidgets() != null) {
                        for (Widget widget : dashBoard.getWidgets()) {
                            Integer i = boards.get(widget.getClass().getSimpleName());
                            if (i == null) {
                                i = 0;
                            }
                            boards.put(widget.getClass().getSimpleName(), ++i);
                        }
                    }
                }
            } else {
                System.out.println(JsonParser.toJson(user));
            }
        }

        for (Map.Entry<String, Integer> entry : boards.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

}

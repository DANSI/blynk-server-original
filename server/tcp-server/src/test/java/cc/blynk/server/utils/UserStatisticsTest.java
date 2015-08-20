package cc.blynk.server.utils;

import cc.blynk.server.TestBase;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.Widget;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

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
                //System.out.println(JsonParser.toJson(user));
            }
        }

        for (Map.Entry<String, Integer> entry : boards.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    @Test
    public void printDashFilling() {
        System.out.println();
        System.out.println("Dashboard Space Usage :");

        List<Integer> all = new ArrayList<>();
        for (User user : users.values()) {
            if (user.getProfile().getDashBoards() != null) {
                for (DashBoard dashBoard : user.getProfile().getDashBoards()) {
                    if (dashBoard.getWidgets() != null && dashBoard.getWidgets().length > 3) {
                        int sum = 0;
                        for (Widget widget : dashBoard.getWidgets()) {
                           sum += widget.height * widget.width;
                        }
                        all.add(sum);
                    }
                }
            }
        }

        Collections.sort(all);

        System.out.println(all.get(all.size() / 2));
        System.out.println(all.get(all.size() / 2) * 100 / 72 + "%");
        //System.out.println(usersCounter);
        //System.out.println(dashes);
        //System.out.println("Average filled square per dash : " + (sum / dashes));
        //System.out.println("Percentage : " + (int)((sum / dashes) * 100 / 72));

    }

}

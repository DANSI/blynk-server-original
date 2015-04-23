package сс.blynk.server.core.administration.actions;

import cc.blynk.common.enums.Command;
import cc.blynk.server.core.administration.actions.ActivityMonitor;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.model.auth.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.04.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ActivityMonitorTest {

    @Mock
    private UserRegistry userRegistry;

    @Mock
    private User user;

    @Mock
    private Map<String, User> users;



    @Test
    public void testPassChanged() {
        ActivityMonitor activityMonitor = new ActivityMonitor();

        List<User> usersList = new ArrayList<User>() {
            {
                add(new User("1", "1"));
                add(new User("2", "2"));
            }
        };
        usersList.get(0).incrStat(Command.GET_TOKEN);

        when(userRegistry.getUsers()).thenReturn(users);
        when(users.size()).thenReturn(2);
        when(users.values()).thenReturn(usersList);

        List<String> repsonse = activityMonitor.execute(userRegistry, null);

        assertNotNull(repsonse);
        assertEquals(4, repsonse.size());
        assertTrue(repsonse.get(1).contains("1 - "));
        assertEquals("ok\n", repsonse.get(3));
    }

    @Test
    public void testNoUser() {
        ActivityMonitor activityMonitor = new ActivityMonitor();

        when(userRegistry.getUsers()).thenReturn(users);
        when(users.size()).thenReturn(2);
        when(users.values()).thenReturn(Collections.emptyList());

        List<String> repsonse = activityMonitor.execute(userRegistry, null);

        assertNotNull(repsonse);
        assertEquals(2, repsonse.size());
        assertEquals("ok\n", repsonse.get(1));
    }


}

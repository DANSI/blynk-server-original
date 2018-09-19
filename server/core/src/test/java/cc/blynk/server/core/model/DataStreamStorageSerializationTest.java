package cc.blynk.server.core.model;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.storage.MultiPinStorageValue;
import cc.blynk.server.core.model.storage.MultiPinStorageValueType;
import cc.blynk.server.core.model.storage.PinPropertyStorageKey;
import cc.blynk.server.core.model.storage.PinStorageKey;
import cc.blynk.server.core.model.storage.PinStorageValue;
import cc.blynk.server.core.model.storage.SinglePinStorageValue;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.11.16.
 */
public class DataStreamStorageSerializationTest {

    @Test
    public void testSerializeSingleEmptyValue() {
        User user = new User();
        user.email = "123";
        user.profile = new Profile();
        user.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        user.lastModifiedTs = 0;
        user.profile.dashBoards[0].pinsStorage = new HashMap<>();
        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey, new SinglePinStorageValue());

        String result = user.toString();
        assertTrue(result.contains("\"0-v0\":\"\""));
    }

    @Test
    public void testSerializeSingleValue() {
        User user = new User();
        user.email = "123";
        user.profile = new Profile();
        user.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        user.lastModifiedTs = 0;
        user.profile.dashBoards[0].pinsStorage = new HashMap<>();
        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);
        PinStorageKey pinStorageKey2 = new PinStorageKey(0, PinType.DIGITAL, (byte) 1);
        PinPropertyStorageKey pinStorageKey3 = new PinPropertyStorageKey(0, PinType.VIRTUAL, (byte) 0, WidgetProperty.LABEL);
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey, new SinglePinStorageValue("1"));
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey2,new SinglePinStorageValue("2"));
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey3, new SinglePinStorageValue("3"));

        String result = user.toString();
        assertTrue(result.contains("\"0-v0\":\"1\""));
        assertTrue(result.contains("\"0-d1\":\"2\""));
        assertTrue(result.contains("\"0-v0-label\":\"3\""));
    }

    @Test
    public void testSerializeMultiValueEmpty() {
        User user = new User();
        user.email = "123";
        user.profile = new Profile();
        user.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        user.lastModifiedTs = 0;
        user.profile.dashBoards[0].pinsStorage = new HashMap<>();
        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);
        PinStorageValue pinStorageValue = new MultiPinStorageValue(MultiPinStorageValueType.LCD);
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey, pinStorageValue);

        String result = user.toString();
        assertTrue(result.contains("\"0-v0\":{\"type\":\"LCD\"}"));
    }

    @Test
    public void testSerializeMultiValueWithSingleValue() {
        User user = new User();
        user.email = "123";
        user.profile = new Profile();
        user.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        user.lastModifiedTs = 0;
        user.profile.dashBoards[0].pinsStorage = new HashMap<>();
        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);
        PinStorageValue pinStorageValue = new MultiPinStorageValue(MultiPinStorageValueType.LCD);
        pinStorageValue.update("1");
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey, pinStorageValue);

        String result = user.toString();
        assertTrue(result.contains("\"0-v0\":{\"type\":\"LCD\",\"values\":[\"1\"]}"));
    }

    @Test
    public void testSerializeMultiValueWithMultipleValues() {
        User user = new User();
        user.email = "123";
        user.profile = new Profile();
        user.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        user.lastModifiedTs = 0;
        user.profile.dashBoards[0].pinsStorage = new HashMap<>();
        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);
        PinStorageValue pinStorageValue = new MultiPinStorageValue(MultiPinStorageValueType.LCD);
        pinStorageValue.update("1");
        pinStorageValue.update("2");
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey, pinStorageValue);

        String result = user.toString();
        assertTrue(result.contains("\"0-v0\":{\"type\":\"LCD\",\"values\":[\"1\",\"2\"]}"));
    }

    @Test
    public void testSerializeMultiValueWithMultipleValuesAndLimit() {
        User user = new User();
        user.email = "123";
        user.profile = new Profile();
        user.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        user.lastModifiedTs = 0;
        user.profile.dashBoards[0].pinsStorage = new HashMap<>();
        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);
        PinStorageValue pinStorageValue = new MultiPinStorageValue(MultiPinStorageValueType.LCD);
        pinStorageValue.update("1");
        pinStorageValue.update("2");
        pinStorageValue.update("3");
        pinStorageValue.update("4");
        pinStorageValue.update("5");
        pinStorageValue.update("6");
        pinStorageValue.update("7");
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey, pinStorageValue);

        String result = user.toString();
        assertTrue(result.contains("\"0-v0\":{\"type\":\"LCD\",\"values\":[\"2\",\"3\",\"4\",\"5\",\"6\",\"7\"]}"));
    }

    @Test
    public void testSerializeMultiValueWithNilValue() {
        User user = new User();
        user.email = "123";
        user.profile = new Profile();
        user.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        user.lastModifiedTs = 0;
        user.profile.dashBoards[0].pinsStorage = new HashMap<>();
        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);
        PinStorageValue pinStorageValue = new MultiPinStorageValue(MultiPinStorageValueType.LCD);
        pinStorageValue.update("\0");
        user.profile.dashBoards[0].pinsStorage.put(pinStorageKey, pinStorageValue);

        String result = user.toString();
        assertTrue(result.contains("\"0-v0\":{\"type\":\"LCD\",\"values\":[\"\\u0000\"]}"));
    }

    @Test
    public void testDeserializeSingleValue() throws Exception{
        String expectedString = "{\"email\":\"123\",\"appName\":\"Blynk\",\"lastModifiedTs\":0,\"lastLoggedAt\":0,\"profile\":{\"dashBoards\":[{\"id\":0,\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false," +
                "\"pinsStorage\":{\"0-v0\":\"1\",\"0-d111\":\"2\", \"0-v0-label\":\"3\"}" +
                "}]},\"isFacebookUser\":false,\"energy\":2000,\"id\":\"123-Blynk\"}";

        User user = JsonParser.parseUserFromString(expectedString);
        assertNotNull(user);
        assertEquals(3, user.profile.dashBoards[0].pinsStorage.size());

        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);
        PinStorageKey pinStorageKey2 = new PinStorageKey(0, PinType.DIGITAL, (byte) 111);
        PinPropertyStorageKey pinStorageKey3 = new PinPropertyStorageKey(0, PinType.VIRTUAL, (byte) 0, WidgetProperty.LABEL);

        assertEquals("1", ((SinglePinStorageValue) user.profile.dashBoards[0].pinsStorage.get(pinStorageKey)).value);
        assertEquals("2", ((SinglePinStorageValue) user.profile.dashBoards[0].pinsStorage.get(pinStorageKey2)).value);
        assertEquals("3", ((SinglePinStorageValue) user.profile.dashBoards[0].pinsStorage.get(pinStorageKey3)).value);
    }

    @Test
    public void testDeserializeMultiValue() throws Exception {
        String expectedString = "{\"email\":\"123\",\"appName\":\"Blynk\",\"lastModifiedTs\":0,\"lastLoggedAt\":0,\"profile\":{\"dashBoards\":[{\"id\":0,\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false," +
                "\"pinsStorage\":{\"0-v0\":{\"type\":\"LCD\",\"values\":[\"1\",\"2\"]}}" +
                "}]},\"isFacebookUser\":false,\"energy\":2000,\"id\":\"123-Blynk\"}";

        User user = JsonParser.parseUserFromString(expectedString);
        assertNotNull(user);
        assertEquals(1, user.profile.dashBoards[0].pinsStorage.size());

        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);

        assertEquals("1", ((MultiPinStorageValue) user.profile.dashBoards[0].pinsStorage.get(pinStorageKey)).values.poll());
        assertEquals("2", ((MultiPinStorageValue) user.profile.dashBoards[0].pinsStorage.get(pinStorageKey)).values.poll());
    }

    @Test
    public void testDeserializeEmptyMultiValue() throws Exception {
        String expectedString = "{\"email\":\"123\",\"appName\":\"Blynk\",\"lastModifiedTs\":0,\"lastLoggedAt\":0,\"profile\":{\"dashBoards\":[{\"id\":0,\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false," +
                "\"pinsStorage\":{\"0-v0\":{\"type\":\"LCD\"}}" +
                "}]},\"isFacebookUser\":false,\"energy\":2000,\"id\":\"123-Blynk\"}";

        User user = JsonParser.parseUserFromString(expectedString);
        assertNotNull(user);
        assertEquals(1, user.profile.dashBoards[0].pinsStorage.size());

        PinStorageKey pinStorageKey = new PinStorageKey(0, PinType.VIRTUAL, (byte) 0);

        assertNotNull(user.profile.dashBoards[0].pinsStorage.get(pinStorageKey));
        assertEquals(0, ((MultiPinStorageValue) user.profile.dashBoards[0].pinsStorage.get(pinStorageKey)).values.size());
    }

}

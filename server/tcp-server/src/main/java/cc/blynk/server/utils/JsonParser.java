package cc.blynk.server.utils;

import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.notifications.GCMMessage;
import cc.blynk.server.notifications.GCMResponseMessage;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 15:31
 */
public final class JsonParser {

    //it is threadsafe
    public static final ObjectMapper mapper = init();
    private static final Logger log = LogManager.getLogger(JsonParser.class);
    private static final ObjectReader userReader = mapper.reader(User.class);
    private static final ObjectReader profileReader = mapper.reader(Profile.class);
    private static final ObjectReader gcmResponseReader = mapper.reader(GCMResponseMessage.class);

    private static final ObjectWriter userWriter = mapper.writerFor(User.class);
    private static final ObjectWriter profileWriter = mapper.writerFor(Profile.class);
    private static final ObjectWriter gcmWriter = mapper.writerFor(GCMMessage.class);

    private static ObjectMapper init() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public static String toJson(User user) {
        return toJson(userWriter, user);
    }

    public static String toJson(GCMMessage message) {
        return toJson(gcmWriter, message);
    }

    public static String toJson(Profile profile) {
        return toJson(profileWriter, profile);
    }

    private static String toJson(ObjectWriter writer, Object o) {
        try {
            return writer.writeValueAsString(o);
        } catch (Exception e) {
            log.error("Error jsoning object.", e);
        }
        return "{}";
    }

    public static User parseUser(String reader) throws IOException {
        User user = userReader.readValue(reader);
        user.initQuota();
        return user;
    }

    public static GCMResponseMessage parseGCMResponse(String reader) throws IOException {
        return gcmResponseReader.readValue(reader);
    }

    public static Profile parseProfile(String reader, int id) {
        try {
            Profile profile = profileReader.readValue(reader);
            profile.calcGraphPins();
            return profile;
        } catch (IOException e) {
            throw new IllegalCommandException("Error parsing user profile. Reason : " + e.getMessage(), id);
        }
    }

    //only for tests
    public static Profile parseProfile(InputStream reader) {
        try {
            Profile profile = profileReader.readValue(reader);
            profile.calcGraphPins();
            return profile;
        } catch (IOException e) {
            throw new IllegalCommandException("Error parsing user profile. Reason : " + e.getMessage(), 1);
        }
    }

}

package cc.blynk.utils;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 15:31
 */
public final class JsonParser {

    //it is threadsafe
    public static final ObjectMapper mapper = init();
    private static final Logger log = LogManager.getLogger(JsonParser.class);
    private static final ObjectReader userReader = mapper.readerFor(User.class);
    private static final ObjectReader profileReader = mapper.readerFor(Profile.class);
    private static final ObjectReader dashboardReader = mapper.readerFor(DashBoard.class);
    private static final ObjectReader widgetReader = mapper.readerFor(Widget.class);

    private static final ObjectWriter userWriter = mapper.writerFor(User.class);
    private static final ObjectWriter profileWriter = mapper.writerFor(Profile.class);
    private static final ObjectWriter dashboardWriter = mapper.writerFor(DashBoard.class);

    public static ObjectMapper init() {
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

    public static String toJson(Profile profile) {
        return toJson(profileWriter, profile);
    }

    public static String toJson(DashBoard dashBoard) {
        return toJson(dashboardWriter, dashBoard);
    }

    public static String toJson(ObjectWriter writer, Object o) {
        try {
            return writer.writeValueAsString(o);
        } catch (Exception e) {
            log.error("Error jsoning object.", e);
        }
        return "{}";
    }

    public static String toJson(Map<?, ?> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Error jsoning object.", e);
        }
        return "{}";
    }

    public static String toJson(Collection<?> list) {
        try {
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Error jsoning object.", e);
        }
        return "[]";
    }

    public static <T> T readAny(String val, Class<T> c) {
        try {
            return mapper.readValue(val, c);
        } catch (Exception e) {
            log.error("Error reading json object.", e);
        }
        return null;
    }

    public static User parseUserFromFile(File userFile) throws IOException {
        return userReader.readValue(userFile);
    }

    public static User parseUserFromString(String userString) throws IOException {
        return userReader.readValue(userString);
    }

    public static DashBoard parseDashboard(String reader) {
        try {
            return dashboardReader.readValue(reader);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IllegalCommandBodyException("Error parsing dashboard.");
        }
    }

    public static Profile parseProfile(String reader) {
        try {
            return profileReader.readValue(reader);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IllegalCommandBodyException("Error parsing user profile.");
        }
    }

    public static Widget parseWidget(String reader) {
        try {
            return widgetReader.readValue(reader);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IllegalCommandBodyException("Error parsing widget.");
        }
    }

    //only for tests
    public static Profile parseProfile(InputStream reader) {
        try {
            return profileReader.readValue(reader);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IllegalCommandBodyException("Error parsing user profile.");
        }
    }

    public static String valueToJsonAsString(String[] values) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (String value : values) {
            sj.add(makeJsonStringValue(value));
        }
        return sj.toString();
    }

    public static String valueToJsonAsString(String value) {
        return "[\"" + value  + "\"]";
    }

    private static String makeJsonStringValue(String value) {
        return "\"" + value  + "\"";
    }

}

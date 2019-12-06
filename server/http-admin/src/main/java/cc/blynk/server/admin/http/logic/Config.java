package cc.blynk.server.admin.http.logic;

import cc.blynk.server.core.model.serialization.JsonParser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.04.16.
 */
public class Config {

    public String name;
    public String body;

    Config() {
    }

    Config(String name) {
        this.name = name;
    }

    Config(String name, String body) {
        this.name = name;
        this.body = body;
    }

    Config(String name, Properties properties) {
        this.name = name;
        //return only editable options
        this.body = getPropertyAsString(properties);
    }

    private static String getPropertyAsString(Properties prop) {
        StringWriter writer = new StringWriter();
        prop.list(new PrintWriter(writer));
        return writer.getBuffer().replace(0, "-- listing properties --\n".length(), "").toString();
    }

    @Override
    public String toString() {
        try {
            return JsonParser.toJson(this);
        } catch (Exception e) {
            return "{}";
        }
    }
}

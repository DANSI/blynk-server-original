package cc.blynk.server.admin.http.logic;

import cc.blynk.utils.JsonParser;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.04.16.
 */
public class Config {

    String name;
    String body;

    public Config() {
    }

    public Config(String name) {
        this.name = name;
    }

    public Config(String name, String body) {
        this.name = name;
        this.body = body;
    }

    @Override
    public String toString() {
        try {
            return JsonParser.mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }
}

package cc.blynk.server.admin.http.logic;

import cc.blynk.core.http.CookiesBaseHttpHandler;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.GET;
import cc.blynk.core.http.annotation.PUT;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.core.http.annotation.PathParam;
import cc.blynk.core.http.annotation.QueryParam;
import cc.blynk.server.Holder;
import cc.blynk.server.Limits;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.utils.http.MediaType;
import cc.blynk.utils.properties.ServerProperties;
import io.netty.channel.ChannelHandler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static cc.blynk.core.http.Response.appendTotalCountHeader;
import static cc.blynk.core.http.Response.badRequest;
import static cc.blynk.core.http.Response.ok;
import static cc.blynk.server.db.DBManager.DB_PROPERTIES_FILENAME;
import static cc.blynk.utils.AdminHttpUtil.sort;
import static cc.blynk.utils.FileLoaderUtil.TOKEN_MAIL_BODY;
import static cc.blynk.utils.properties.GCMProperties.GCM_PROPERTIES_FILENAME;
import static cc.blynk.utils.properties.MailProperties.MAIL_PROPERTIES_FILENAME;
import static cc.blynk.utils.properties.ServerProperties.SERVER_PROPERTIES_FILENAME;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/config")
@ChannelHandler.Sharable
public class ConfigsLogic extends CookiesBaseHttpHandler {

    private final Limits limits;
    private final ServerProperties serverProperties;

    public ConfigsLogic(Holder holder, String rootPath) {
        super(holder, rootPath);
        this.limits = holder.limits;
        this.serverProperties = holder.props;
    }

    @GET
    @Path("")
    public Response getConfigs(@QueryParam("_filters") String filterParam,
                             @QueryParam("_page") int page,
                             @QueryParam("_perPage") int size,
                             @QueryParam("_sortField") String sortField,
                             @QueryParam("_sortDir") String sortOrder) {

        List<Config> configs = new ArrayList<>();
        configs.add(new Config(SERVER_PROPERTIES_FILENAME));
        configs.add(new Config(MAIL_PROPERTIES_FILENAME));
        configs.add(new Config(GCM_PROPERTIES_FILENAME));
        configs.add(new Config(DB_PROPERTIES_FILENAME));
        configs.add(new Config("twitter4j.properties"));
        configs.add(new Config(TOKEN_MAIL_BODY));

        return appendTotalCountHeader(
                                ok(sort(configs, sortField, sortOrder), page, size), configs.size()
        );
    }

    @GET
    @Path("/{name}")
    public Response getConfigByName(@PathParam("name") String name) {
        switch (name) {
            case TOKEN_MAIL_BODY:
                return ok(new Config(name, limits.tokenBody).toString());
            case SERVER_PROPERTIES_FILENAME :
                return ok(new Config(name, serverProperties).toString());
            default :
                return badRequest();
        }
    }


    @PUT
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public Response updateConfig(@PathParam("name") String name,
                               Config updatedConfig) {

        log.info("Updating config {}. New body : ", name);
        log.info("{}", updatedConfig.body);

        switch (name) {
            case TOKEN_MAIL_BODY:
                limits.tokenBody = updatedConfig.body;
                break;
            case SERVER_PROPERTIES_FILENAME :
                Properties properties = readPropertiesFromString(updatedConfig.body);
                serverProperties.putAll(properties);
                break;
        }

        return ok(updatedConfig.toString());
    }

    private static Properties readPropertiesFromString(String propertiesAsString) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(propertiesAsString));
        } catch (IOException e) {
            log.error("Error reading properties as string. {}", e.getMessage());
        }
        return properties;
    }

    /**
     * The Blynk Project.
     * Created by Dmitriy Dumanskiy.
     * Created on 04.04.16.
     */
    private static class Config {

        String name;
        String body;

        Config() {
        }

        Config(String name) {
            this.name = name;
        }

        Config(String name, String body) {
            this.name = name;
            this.body = body;
        }

        Config(String name, ServerProperties serverProperties) {
            this.name = name;
            //return only editable options
            this.body = makeProperties(serverProperties,
                    "allowed.administrator.ips",
                    "user.dashboard.max.limit",
                    "user.profile.max.size");
        }

        private static String makeProperties(ServerProperties properties, String... propertyNames) {
            StringBuilder sb = new StringBuilder();
            for (String name : propertyNames) {
                sb.append(name).append(" = ").append(properties.getProperty(name)).append("\n");
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            try {
                return JsonParser.MAPPER.writeValueAsString(this);
            } catch (Exception e) {
                return "{}";
            }
        }
    }
}

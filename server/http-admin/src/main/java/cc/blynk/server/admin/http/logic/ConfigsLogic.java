package cc.blynk.server.admin.http.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.handlers.http.rest.Response;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.utils.ServerProperties;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

import static cc.blynk.server.handlers.http.rest.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("/config")
public class ConfigsLogic extends BaseLogic {

    private final BlockingIOProcessor blockingIOProcessor;

    public ConfigsLogic(BlockingIOProcessor blockingIOProcessor) {
        this.blockingIOProcessor = blockingIOProcessor;
    }

    @GET
    @Path("")
    public Response getConfigs(@QueryParam("_filters") String filterParam,
                             @QueryParam("_page") int page,
                             @QueryParam("_perPage") int size,
                             @QueryParam("_sortField") String sortField,
                             @QueryParam("_sortDir") String sortOrder) {

        List<Config> configs = new ArrayList<>();
        configs.add(new Config(ServerProperties.SERVER_PROPERTIES_FILENAME));
        configs.add(new Config(MailWrapper.MAIL_PROPERTIES_FILENAME));
        configs.add(new Config(GCMWrapper.GCM_PROPERTIES_FILENAME));
        configs.add(new Config(DBManager.DB_PROPERTIES_FILENAME));
        configs.add(new Config("twitter4j.properties"));
        configs.add(new Config("token_mail_body.txt"));

        return appendTotalCountHeader(
                                ok(sort(configs, sortField, sortOrder), page, size), configs.size()
        );
    }

    @GET
    @Path("/{name}")
    public Response getUserByName(@PathParam("name") String name) {
        if ("token_mail_body.txt".equals(name)) {
            return ok(new Config(name, blockingIOProcessor.tokenBody).toString());
        }

        return badRequest();
    }


    @PUT
    @Consumes(value = MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public Response updateUser(@PathParam("name") String name,
                               Config updatedConfig) {

        if ("token_mail_body.txt".equals(name)) {
            log.info("Updating config {}. New body : {}", name, updatedConfig.body);
            blockingIOProcessor.tokenBody = updatedConfig.body;
        }

        return ok(updatedConfig.toString());
    }

}

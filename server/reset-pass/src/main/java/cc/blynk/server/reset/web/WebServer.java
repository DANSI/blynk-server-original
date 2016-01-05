package cc.blynk.server.reset.web;

import cc.blynk.server.reset.web.entities.TokensPool;
import cc.blynk.server.reset.web.handlers.ResetPasswordHandler;
import cc.blynk.utils.ParseUtil;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/05/2015.
 */
class WebServer {

    public static void main(String[] args) throws Exception {
        System.setProperty("logs.folder", ".");
        args = (args.length == 0 ? new String[]{"localhost", "8083"} : args);
        String url = String.format("http://%s", args[0] == null ? "localhost" : args[0]);
        int port = ParseUtil.parseInt(args[1] == null ? "80" : args[1]);
        URI baseUri = UriBuilder.fromUri(url + "/").port(port).build();
        ResourceConfig config = new ResourceConfig().register(new ResetPasswordHandler(url, port, new TokensPool()));
        JdkHttpServerFactory.createHttpServer(baseUri, config);
    }

}

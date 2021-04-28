package cc.blynk.server.launcher;

import cc.blynk.server.Holder;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.server.servers.hardware.MQTTHardwareServer;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.JarUtil;
import cc.blynk.utils.LoggerUtil;
import cc.blynk.utils.SHA256Util;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.properties.GCMProperties;
import cc.blynk.utils.properties.MailProperties;
import cc.blynk.utils.properties.ServerProperties;
import cc.blynk.utils.properties.SmsProperties;
import cc.blynk.utils.properties.TwitterProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.net.BindException;
import java.security.Security;
import java.util.Map;

/**
 * Entry point for server launch.
 *
 * By default starts 4 servers on different ports:
 *
 * 1 server socket for HTTP API, Blynk hardware protocol, web sockets (8080 default)
 * 1 server socket for HTTPS API, Blynk app protocol, hardware secured blynkapp, web sockets (9443 default)
 * 1 server socket for MQTT (8440 default)
 *
 * In addition launcher start all related to business logic threads like saving user profiles thread, timers
 * processing thread, properties reload thread and shutdown hook tread.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/16/2015.
 */
public final class ServerLauncher {

    //required for QR generation
    static {
        System.setProperty("java.awt.headless", "true");
    }

    private ServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> cmdProperties = ArgumentsParser.parse(args);

        ServerProperties serverProperties = new ServerProperties(cmdProperties);

        LoggerUtil.configureLogging(serverProperties);

        //required for logging dynamic context
        System.setProperty("data.folder", serverProperties.getProperty("data.folder"));

        //required to avoid dependencies within model to server.properties
        setGlobalProperties(serverProperties);

        MailProperties mailProperties = new MailProperties(cmdProperties);
        SmsProperties smsProperties = new SmsProperties(cmdProperties);
        GCMProperties gcmProperties = new GCMProperties(cmdProperties);
        TwitterProperties twitterProperties = new TwitterProperties(cmdProperties);

        Security.addProvider(new BouncyCastleProvider());

        boolean restore = Boolean.parseBoolean(cmdProperties.get(ArgumentsParser.RESTORE_OPTION));
        start(serverProperties, mailProperties, smsProperties,
                gcmProperties, twitterProperties, restore);
    }

    private static void setGlobalProperties(ServerProperties serverProperties) {
        Map<String, String> globalProps = Map.of(
                "terminal.strings.pool.size", "25",
                "initial.energy", "2000",
                "table.rows.pool.size", "100",
                "csv.export.data.points.max", "43200",
                "map.strings.pool.size", "25"
        );

        for (var entry : globalProps.entrySet()) {
            String name = entry.getKey();
            String value = serverProperties.getProperty(name, entry.getValue());
            System.setProperty(name, value);
        }
    }

    private static void start(ServerProperties serverProperties, MailProperties mailProperties,
                              SmsProperties smsProperties, GCMProperties gcmProperties,
                              TwitterProperties twitterProperties,
                              boolean restore) {
        Holder holder = new Holder(serverProperties,
                mailProperties, smsProperties, gcmProperties, twitterProperties,
                restore);

        BaseServer[] servers = new BaseServer[] {
                new HardwareAndHttpAPIServer(holder),
                new MobileAndHttpsServer(holder),
                new MQTTHardwareServer(holder)
        };

        if (startServers(servers)) {
            //Launching all background jobs.
            JobLauncher.start(holder, servers);

            System.out.println();
            System.out.println("Blynk Server " + JarUtil.getServerVersion() + " successfully started.");
            String path = new File(System.getProperty("logs.folder")).getAbsolutePath().replace("/./", "/");
            System.out.println("All server output is stored in folder '" + path + "' file.");

            holder.sslContextHolder.generateInitialCertificates(holder.props);

            createSuperUser(holder);
        }
    }

    private static void createSuperUser(Holder holder) {
        ServerProperties props = holder.props;
        String url = props.getAdminUrl(props.host);
        String email = props.getProperty("admin.email", "admin@blynk.cc");
        String pass = props.getProperty("admin.pass");

        if (!holder.userDao.isSuperAdminExists()) {
            if (pass == null || pass.isEmpty()) {
                System.out.println("Admin password not specified. Random password generated.");
                pass = StringUtils.randomPassword(24);
            }

            System.out.println("Your Admin url is " + url);
            System.out.println("Your Admin login email is " + email);
            System.out.println("Your Admin password is " + pass);

            String hash = SHA256Util.makeHash(pass, email);
            holder.userDao.add(email, hash, AppNameUtil.BLYNK, true);
        }
    }

    private static boolean startServers(BaseServer[] servers) {
        //start servers
        try {
            for (BaseServer server : servers) {
                server.start();
            }
            return true;
        } catch (BindException bindException) {
            System.out.println("Server ports are busy. Most probably server already launched. See "
                    + new File(System.getProperty("logs.folder")).getAbsolutePath() + " for more info.");
        } catch (Exception e) {
            System.out.println("Error starting Blynk server. Stopping.");
        }

        return false;
    }

}

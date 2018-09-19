package cc.blynk.server.launcher;

import cc.blynk.cli.CommandLine;
import cc.blynk.cli.DefaultParser;
import cc.blynk.cli.Options;
import cc.blynk.cli.ParseException;

import java.util.HashMap;
import java.util.Map;

import static cc.blynk.utils.properties.MailProperties.MAIL_PROPERTIES_FILENAME;
import static cc.blynk.utils.properties.ServerProperties.SERVER_PROPERTIES_FILENAME;
import static cc.blynk.utils.properties.SmsProperties.SMS_PROPERTIES_FILENAME;

/**
 * Simple class for command line arguments parsing.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.03.15.
 */
final class ArgumentsParser {

    private static final Options options;

    private static final String HARDWARE_PORT_OPTION = "hardPort";
    private static final String APPLICATION_PORT_OPTION = "appPort";
    private static final String WORKER_THREADS_OPTION = "workerThreads";
    private static final String DATA_FOLDER_OPTION = "dataFolder";
    private static final String SERVER_CONFIG_PATH_OPTION = "serverConfig";
    private static final String MAIL_CONFIG_PATH_OPTION = "mailConfig";
    private static final String SMS_CONFIG_PATH_OPTION = "smsConfig";
    static final String RESTORE_OPTION = "restore";

    static  {
        options = new Options()
               .addOption(HARDWARE_PORT_OPTION, true, "Hardware server port.")
               .addOption(APPLICATION_PORT_OPTION, true, "Application server port.")
               .addOption(WORKER_THREADS_OPTION, true, "Server worker threads.")
               .addOption(DATA_FOLDER_OPTION, true, "Folder where user profiles will be stored.")
               .addOption(SERVER_CONFIG_PATH_OPTION, true, "Path to server.properties config file.")
               .addOption(MAIL_CONFIG_PATH_OPTION, true, "Path to mail.properties config file.")
               .addOption(SMS_CONFIG_PATH_OPTION, true, "Path to sms.properties config file.")
               .addOption(RESTORE_OPTION, false, "Restore data from DB.");
    }

    private ArgumentsParser() {
    }

    /**
     * Simply parsers command line arguments and sets it to server properties for future use.
     *
     * @param args - command line arguments
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static Map<String, String> parse(String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(options, args);

        String hardPort = cmd.getOptionValue(HARDWARE_PORT_OPTION);
        String appPort = cmd.getOptionValue(APPLICATION_PORT_OPTION);
        String workerThreadsString = cmd.getOptionValue(WORKER_THREADS_OPTION);
        String dataFolder = cmd.getOptionValue(DATA_FOLDER_OPTION);
        String serverConfigPath = cmd.getOptionValue(SERVER_CONFIG_PATH_OPTION);
        String mailConfigPath = cmd.getOptionValue(MAIL_CONFIG_PATH_OPTION);
        String smsConfigPath = cmd.getOptionValue(SMS_CONFIG_PATH_OPTION);
        boolean restore = cmd.hasOption(RESTORE_OPTION);

        Map<String, String> properties = new HashMap<>();

        if (hardPort != null) {
            Integer.parseInt(hardPort);
            properties.put("http.port", hardPort);
        }

        if (appPort != null) {
            Integer.parseInt(appPort);
            properties.put("https.port", appPort);
        }

        if (workerThreadsString != null) {
            Integer.parseInt(workerThreadsString);
            properties.put("server.worker.threads", workerThreadsString);
        }
        if (dataFolder != null) {
            properties.put("data.folder", dataFolder);
        }
        if (serverConfigPath != null) {
            properties.put(SERVER_PROPERTIES_FILENAME, serverConfigPath);
        }
        if (mailConfigPath != null) {
            properties.put(MAIL_PROPERTIES_FILENAME, mailConfigPath);
        }
        if (smsConfigPath != null) {
            properties.put(SMS_PROPERTIES_FILENAME, smsConfigPath);
        }

        properties.put(RESTORE_OPTION, Boolean.toString(restore));

        return properties;
    }

}

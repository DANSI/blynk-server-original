package cc.blynk.server.launcher;

import cc.blynk.utils.ParseUtil;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Properties;

/**
 * Simple class for command line arguments parsing.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.03.15.
 */
class ArgumentsParser {

    private static final Options options;

    private static final String HARDWARE_PORT_OPTION = "hardPort";
    private static final String APPLICATION_PORT_OPTION = "appPort";
    private static final String WORKER_THREADS_OPTION = "workerThreads";
    private static final String DATA_FOLDER_OPTION = "dataFolder";

    static  {
        options = new Options();
        options.addOption(HARDWARE_PORT_OPTION, true, "Hardware server port.")
               .addOption(APPLICATION_PORT_OPTION, true, "Application server port.")
               .addOption(WORKER_THREADS_OPTION, true, "Server worker threads.")
               .addOption(DATA_FOLDER_OPTION, true, "Folder where user profiles will be stored.");
    }

    /**
     * Simply parsers command line arguments and sets it to server properties for future use.
     *
     * @param args - command line arguments
     * @param serverProperties - server properties read from file
     * @throws ParseException
     */
    static void parse(String[] args, Properties serverProperties) throws ParseException {
        CommandLine cmd = new BasicParser().parse(options, args);

        String hardPort = cmd.getOptionValue(HARDWARE_PORT_OPTION);
        String appPort = cmd.getOptionValue(APPLICATION_PORT_OPTION);
        String workerThreadsString = cmd.getOptionValue(WORKER_THREADS_OPTION);
        String dataFolder = cmd.getOptionValue(DATA_FOLDER_OPTION);

        if (hardPort != null) {
            ParseUtil.parseInt(hardPort);
            serverProperties.put("hardware.default.port", hardPort);
        }
        if (appPort != null) {
            ParseUtil.parseInt(appPort);
            serverProperties.put("app.ssl.port", appPort);
        }
        if (workerThreadsString != null) {
            ParseUtil.parseInt(workerThreadsString);
            serverProperties.put("server.worker.threads", workerThreadsString);
        }
        if (dataFolder != null) {
            serverProperties.put("data.folder", dataFolder);
        }
    }

}

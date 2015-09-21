package cc.blynk.server;

import cc.blynk.common.utils.ParseUtil;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Properties;

/**
 * Simple class for program arguments parsing.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.03.15.
 */
class ArgumentsParser {

    private final Options options;

    private final String HARDWARE_PORT_OPTION = "hardPort";
    private final String APPLICATION_PORT_OPTION = "appPort";
    private final String WORKER_THREADS_OPTION = "workerThreads";
    private final String DATA_FOLDER_OPTION = "workerThreads";

    ArgumentsParser() {
        options = new Options();
        options.addOption(HARDWARE_PORT_OPTION, true, "Hardware server port.")
               .addOption(APPLICATION_PORT_OPTION, true, "Application server port.")
               .addOption(WORKER_THREADS_OPTION, true, "Server worker threads.")
               .addOption(DATA_FOLDER_OPTION, true, "Folder where user profiles will be stored.");
    }


    void processArguments(String[] args, Properties serverProperties) throws ParseException {
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

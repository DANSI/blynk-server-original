package cc.blynk.server.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
public class LoggerUtil {

    /**
     * Sets desired log level from properties.
     *
     * @param level - desired log level. error|info|debug|trace, etc.
     */
    public static void changeLogLevel(String level) {
        Level newLevel = Level.valueOf(level);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration conf = ctx.getConfiguration();
        conf.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(newLevel);
        ctx.updateLoggers(conf);
    }

}

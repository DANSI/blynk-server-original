package cc.blynk.utils;

import cc.blynk.utils.properties.BaseProperties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
public final class LoggerUtil {

    private LoggerUtil() {
    }

    /**
     * - Sets async logger for all logs
     * - Defines logging folder
     * - Sets logging level based on properties
     */
    public static void configureLogging(BaseProperties serverProperties) {
        //required to make all loggers async with LMAX disruptor
        System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("AsyncLogger.RingBufferSize",
                serverProperties.getProperty("async.logger.ring.buffer.size", "2048"));

        //configurable folder for logs via property.
        if (serverProperties.getProperty("logs.folder") == null) {
            System.out.println("logs.folder property is empty.");
            System.exit(1);
        }
        System.setProperty("logs.folder", serverProperties.getProperty("logs.folder"));

        //changing log level based on properties file
        changeLogLevel(serverProperties.getProperty("log.level"));
    }

    /**
     * Sets desired log level from properties.
     *
     * @param level - desired log level. error|info|debug|trace, etc.
     */
    private static void changeLogLevel(String level) {
        Level newLevel = Level.valueOf(level);
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), newLevel);
    }

}

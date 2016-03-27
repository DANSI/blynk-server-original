package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.JsonParser;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.*;


/**
 * Class responsible for saving/reading user data to/from disk.
 *
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 6:53 PM
 */
public class FileManager {

    private static final Logger log = LogManager.getLogger(FileManager.class);

    /**
     * Folder where all user profiles are stored locally.
     */
    private Path dataDir;

    public FileManager(String dataFolder) {
        if (dataFolder == null || "".equals(dataFolder)) {
            dataFolder = Paths.get(System.getProperty("java.io.tmpdir"), "blynk").toString();
        }
        try {
            this.dataDir = createDatadir(dataFolder);
        } catch (RuntimeException e) {
            this.dataDir = createDatadir(Paths.get(System.getProperty("java.io.tmpdir"), "blynk"));
        }

        log.info("Using data dir '{}'", dataDir);
    }

    private static Path createDatadir(String dataFolder) {
        Path dataDir = Paths.get(dataFolder);
        return createDatadir(dataDir);
    }

    private static Path createDatadir(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException ioe) {
            log.error("Error creating data folder '{}'", dataDir);
            throw new RuntimeException("Error creating data folder '" + dataDir + "'");
        }
        return dataDir;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path generateFileName(String userName) {
        return Paths.get(dataDir.toString(), "u_" + userName + ".user");
    }

    public boolean delete(String name) {
        Path file = generateFileName(name);
        return FileUtils.deleteQuietly(file);
    }

    public void overrideUserFile(User user) throws IOException {
        Path file = generateFileName(user.name);
        try (BufferedWriter writer = Files.newBufferedWriter(file, CharsetUtil.UTF_8)) {
            String userString = user.toString();

            writer.write(userString);
        }
    }

    /**
     * Loads all user profiles one by one from disk using dataDir as starting point.
     *
     * @return mapping between username and it's profile.
     */
    public ConcurrentMap<String, User> deserialize() {
        log.debug("Starting reading user DB.");

        File userDBFolder = dataDir.toFile();
        File[] files = userDBFolder.listFiles();

        if (files != null) {
            ConcurrentMap<String, User> tempUsers = Arrays.stream(files).parallel()
                    .filter(File::isFile)
                    .flatMap(file -> {
                        try {
                            User user = JsonParser.parseUserFromFile(file);
                            user.purchaseEnergy(2000);
                            if (user.dashShareTokens == null) {
                                user.dashShareTokens = new HashMap<>();
                            }
                            return Stream.of(user);
                        } catch (IOException ioe) {
                            log.error("Error parsing file '{}'.", file);
                        }
                        return Stream.empty();
                    })
                    .collect(Collectors.toConcurrentMap(User::getName, identity(), (user1, user2) -> user2));

            log.debug("Reading user DB finished.");
            return tempUsers;
        }

        log.debug("Reading user DB finished.");
        return new ConcurrentHashMap<>();
    }

}

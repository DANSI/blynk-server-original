package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;


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

    private static final String DELETED_DATA_DIR_NAME = "deleted";
    private Path deletedDataDir;

    public FileManager(String dataFolder) {
        if (dataFolder == null || "".equals(dataFolder)) {
            dataFolder = Paths.get(System.getProperty("java.io.tmpdir"), "blynk").toString();
        }
        try {
            this.dataDir = createDatadir(dataFolder);
            this.deletedDataDir = createDatadir(Paths.get(dataFolder, DELETED_DATA_DIR_NAME));
        } catch (RuntimeException e) {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "blynk");

            System.out.println("WARNING : could not find folder '" + dataFolder + "'. Please specify correct -dataFolder parameter.");
            System.out.println("Your data may be lost during server restart. Using temp folder : " + tempDir.toString());

            this.dataDir = createDatadir(tempDir);
            this.deletedDataDir = createDatadir(Paths.get(this.dataDir.toString(), DELETED_DATA_DIR_NAME));
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
        return FileUtils.move(file, this.deletedDataDir);
    }

    public void overrideUserFile(User user) throws IOException {
        Path file = generateFileName(user.name);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            String userString = user.toString();

            writer.write(userString);
        }
    }

    /**
     * Loads all user profiles one by one from disk using dataDir as starting point.
     *
     * @return mapping between username and it's profile.
     */
    public ConcurrentMap<UserKey, User> deserialize() {
        log.debug("Starting reading user DB.");

        final File[] files = userFilesList();

        if (files != null) {
            ConcurrentMap<UserKey, User> tempUsers = Arrays.stream(files).parallel()
                    .filter(File::isFile)
                    .flatMap(file -> {
                        try {
                            User user = JsonParser.parseUserFromFile(file);
                            return Stream.of(user);
                        } catch (IOException ioe) {
                            log.error("Error parsing file '{}'. Error : {}", file, ioe.getMessage());
                        }
                        return Stream.empty();
                    })
                    .collect(Collectors.toConcurrentMap(user -> new UserKey(user.name, user.appName), identity(), (user1, user2) -> user2));

            log.debug("Reading user DB finished.");
            return tempUsers;
        }

        log.debug("Reading user DB finished.");
        return new ConcurrentHashMap<>();
    }

    private File[] userFilesList() {
        return dataDir.toFile().listFiles();
    }

    public Map<String, Integer> getUserProfilesSize() {
        final Map<String, Integer> userProfileSize = new HashMap<>();
        final File[] files = userFilesList();
        for (File file : files) {
            if (file.isFile()) {
                String filename = file.getName();
                if (filename.startsWith("u_")){
                    userProfileSize.put(filename, (int) file.length());
                }
            }
        }
        return userProfileSize;
    }

}

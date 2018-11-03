package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.storage.key.DashPinPropertyStorageKey;
import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import cc.blynk.server.core.model.storage.key.PinPropertyStorageKey;
import cc.blynk.server.core.model.storage.key.PinStorageKey;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.createDirectories;
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
    private static final String USER_FILE_EXTENSION = ".user";

    /**
     * Folder where all user profiles are stored locally.
     */
    private Path dataDir;

    private static final String DELETED_DATA_DIR_NAME = "deleted";
    private static final String BACKUP_DATA_DIR_NAME = "backup";
    private static final String CLONE_DATA_DIR_NAME = "clone";
    private Path deletedDataDir;
    private Path backupDataDir;
    private String cloneDataDir;
    private final String host;

    public FileManager(String dataFolder, String host) {
        if (dataFolder == null || dataFolder.isEmpty() || dataFolder.equals("/path")) {
            System.out.println("WARNING : '" + dataFolder + "' does not exists. "
                    + "Please specify correct -dataFolder parameter.");
            dataFolder = Paths.get(System.getProperty("java.io.tmpdir"), "blynk").toString();
            System.out.println("Your data may be lost during server restart. Using temp folder : " + dataFolder);
        }
        try {
            Path dataFolderPath = Paths.get(dataFolder);
            this.dataDir = createDirectories(dataFolderPath);
            this.deletedDataDir = createDirectories(Paths.get(dataFolder, DELETED_DATA_DIR_NAME));
            this.backupDataDir = createDirectories(Paths.get(dataFolder, BACKUP_DATA_DIR_NAME));
            this.cloneDataDir = createDirectories(Paths.get(dataFolder, CLONE_DATA_DIR_NAME)).toString();
        } catch (Exception e) {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "blynk");

            System.out.println("WARNING : could not find folder '" + dataFolder + "'. "
                    + "Please specify correct -dataFolder parameter.");
            System.out.println("Your data may be lost during server restart. Using temp folder : "
                    + tempDir.toString());

            try {
                this.dataDir = createDirectories(tempDir);
                this.deletedDataDir = createDirectories(
                        Paths.get(this.dataDir.toString(), DELETED_DATA_DIR_NAME));
                this.backupDataDir = createDirectories(
                        Paths.get(this.dataDir.toString(), BACKUP_DATA_DIR_NAME));
                this.cloneDataDir = createDirectories(
                        Paths.get(this.dataDir.toString(), CLONE_DATA_DIR_NAME)).toString();
            } catch (Exception ioe) {
                throw new RuntimeException(ioe);
            }
        }

        this.host = host;
        log.info("Using data dir '{}'", dataDir);
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path generateFileName(String email, String appName) {
        return Paths.get(dataDir.toString(), email + "." + appName + USER_FILE_EXTENSION);
    }

    public Path generateBackupFileName(String email, String appName) {
        return Paths.get(backupDataDir.toString(), email + "." + appName + ".user."
                + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
    }

    private Path generateOldFileName(String userName) {
        return Paths.get(dataDir.toString(), "u_" + userName + USER_FILE_EXTENSION);
    }

    public boolean delete(String email, String appName) {
        Path file = generateFileName(email, appName);
        try {
            FileUtils.move(file, this.deletedDataDir);
        } catch (IOException e) {
            log.debug("Failed to move file. {}", e.getMessage());
            return false;
        }
        return true;
    }

    public void overrideUserFile(User user) throws IOException {
        Path path = generateFileName(user.email, user.appName);

        JsonParser.writeUser(path.toFile(), user);

        removeOldFile(user.email);
    }

    private void removeOldFile(String email) {
        //this oldFileName is migration code. should be removed in future versions
        Path oldFileName = generateOldFileName(email);
        try {
            Files.deleteIfExists(oldFileName);
        } catch (Exception e) {
            log.error("Error removing old file. {}", oldFileName, e);
        }
    }

    /**
     * Loads all user profiles one by one from disk using dataDir as starting point.
     *
     * @return mapping between username and it's profile.
     */
    public ConcurrentMap<UserKey, User> deserializeUsers() {
        log.debug("Starting reading user DB.");

        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**" + USER_FILE_EXTENSION);
        ConcurrentMap<UserKey, User> temp;
        try {
            temp = Files.walk(dataDir, 1).parallel()
                    .filter(path -> Files.isRegularFile(path) && pathMatcher.matches(path))
                    .flatMap(path -> {
                        try {
                            User user = JsonParser.parseUserFromFile(path);
                            makeProfileChanges(user);

                            return Stream.of(user);
                        } catch (IOException ioe) {
                            String errorMessage = ioe.getMessage();
                            log.error("Error parsing file '{}'. Error : {}", path, errorMessage);
                            if (errorMessage != null
                                    && (errorMessage.contains("end-of-input")
                                    || errorMessage.contains("Illegal character"))) {
                                return restoreFromBackup(path.getFileName());
                            }
                        }
                        return Stream.empty();
                    })
                    .collect(Collectors.toConcurrentMap(UserKey::new, identity()));
        } catch (Exception e) {
            log.error("Error reading user profiles from disk. {}", e.getMessage());
            throw new RuntimeException(e);
        }

        log.debug("Reading user DB finished.");
        return temp;
    }

    private Stream<User> restoreFromBackup(Path restoreFileNamePath) {
        log.info("Trying to recover from backup...");
        String filename = restoreFileNamePath.toString();
        try {
            File[] files = backupDataDir.toFile().listFiles(
                    (dir, name) -> name.startsWith(filename)
            );

            File backupFile = FileUtils.getLatestFile(files);
            if (backupFile == null) {
                log.info("Didn't find any files for recovery :(.");
                return Stream.empty();
            }
            log.info("Found {}. You are lucky today :).", backupFile.getAbsoluteFile());

            User user = JsonParser.parseUserFromFile(backupFile);
            makeProfileChanges(user);
            //profile saver thread is launched after file manager is initialized.
            //so making sure user profile will be saved
            //this is not very important as profile will be updated by user anyway.
            user.lastModifiedTs = System.currentTimeMillis() + 10 * 1000;
            log.info("Restored.", backupFile.getAbsoluteFile());
            return Stream.of(user);
        } catch (Exception e) {
            //ignore
            log.error("Restoring from backup failed. {}", e.getMessage());
        }
        return Stream.empty();
    }

    //public is for tests only
    public void makeProfileChanges(User user) {
        if (user.email == null) {
            user.email = user.name;
        }
        user.ip = host;
        for (DashBoard dash : user.profile.dashBoards) {
            user.profile.setOfflineDevice(dash);
            if (dash.pinsStorage != null && dash.pinsStorage.size() > 0) {
                int dashId = dash.id;
                for (Map.Entry<PinStorageKey, PinStorageValue> pinsStorageEntry : dash.pinsStorage.entrySet()) {
                    PinStorageKey key = pinsStorageEntry.getKey();
                    DashPinStorageKey dashPinStorageKey;
                    if (key instanceof PinPropertyStorageKey) {
                        dashPinStorageKey = new DashPinPropertyStorageKey(dashId, (PinPropertyStorageKey) key);
                    } else {
                        dashPinStorageKey = new DashPinStorageKey(dashId, key);
                    }
                    PinStorageValue value = pinsStorageEntry.getValue();
                    user.profile.pinsStorage.put(dashPinStorageKey, value);
                }
                dash.pinsStorage = Collections.emptyMap();
            }
        }
    }

    public Map<String, Integer> getUserProfilesSize() {
        Map<String, Integer> userProfileSize = new HashMap<>();
        File[] files = dataDir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(USER_FILE_EXTENSION)) {
                    userProfileSize.put(file.getName(), (int) file.length());
                }
            }
        }
        return userProfileSize;
    }

    public boolean writeCloneProjectToDisk(String token, String json) {
        try {
            Path path = Paths.get(cloneDataDir, token);
            Files.write(path, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            return true;
        } catch (Exception e) {
            log.error("Error saving cloned project to disk. {}", e.getMessage());
        }
        return false;
    }

    public String readClonedProjectFromDisk(String token) {
        Path path = Paths.get(cloneDataDir, token);
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Didn't find cloned project on disk. Path {}. Reason {}", path.toString(), e.getMessage());
        }
        return null;
    }
}

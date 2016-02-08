package cc.blynk.integration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 08.02.16.
 */
public class TokenGenerator {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("/home/doom369/x.csv");
        Set<String> tokens = new HashSet<>();

        for (int i = 0; i < 2000; i++ ) {
            tokens.add(UUID.randomUUID().toString().replace("-", ""));
        }

        System.out.println(tokens.size());
        write(path, tokens);

        System.out.println(UUID.randomUUID().toString().replace("-", ""));
    }

    private static void write(Path path, Set<String> tokens) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String token : tokens) {
                writer.write(token);
                writer.newLine();
            }
        }
    }

}

package cc.blynk.integration.tools;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.FlashedToken;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.03.17.
 */
public class FlahsedTokenGenerator {

    public static void main(String[] args) throws Exception{
        FlashedToken[] flashedTokens = generateTokens("test@blynk.cc", 100, "Grow", 2);
        DBManager dbManager = new DBManager("db-test.properties", new BlockingIOProcessor(4, 100), true);

        dbManager.insertFlashedTokens(flashedTokens);

        for (FlashedToken token : flashedTokens) {
            Path path = Paths.get("/home/doom369/Downloads/grow",  token.token + "_" + token.deviceId + ".jpg");
            generateQR(token.token, path);
        }
    }

    private static FlashedToken[] generateTokens(String email, int count, String appName, int deviceCount) {
        FlashedToken[] flashedTokens = new FlashedToken[count * deviceCount];

        int counter = 0;
        for (int deviceId = 0; deviceId < deviceCount; deviceId++) {
            for (int i = 0; i < count; i++) {
                String token = UUID.randomUUID().toString().replace("-", "");
                flashedTokens[counter++] = new FlashedToken(email, token, appName, 1, deviceId);
                System.out.println("Token : " + token + ", deviceId : " + deviceId + ", appName : " + appName);
            }
        }
        return flashedTokens;
    }

    private static void generateQR(String text, Path outputFile) throws Exception {
        try (OutputStream out = Files.newOutputStream(outputFile)) {
            QRCode.from(text).to(ImageType.JPG).writeTo(out);
        }
    }

}

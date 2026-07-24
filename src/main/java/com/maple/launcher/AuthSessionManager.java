package com.maple.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maple.launcher.services.LauncherPaths;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class AuthSessionManager {

    private static final File SESSION_FILE = getSessionFile();

    private static File getSessionFile() {
        try {
            // Access the APP_DIR field directly from LauncherPaths
            Path dir = LauncherPaths.APP_DIR;
            if (dir != null) {
                return dir.resolve("session.json").toFile();
            }
        } catch (Throwable e) {
            // Fallback if APP_DIR is not accessible
        }
        return new File(System.getProperty("user.home"), ".mapleclient/session.json");
    }

    public static AuthInfos getValidAccessToken() {
        if (!SESSION_FILE.exists()) {
            return null;
        }

        try {
            String encryptedData = readFile(SESSION_FILE);
            String jsonStr = EncryptionUtil.decrypt(encryptedData);
            JsonObject sessionData = JsonParser.parseString(jsonStr).getAsJsonObject();

            String accessToken = sessionData.get("access_token").getAsString();
            String refreshToken = sessionData.has("refresh_token") ? sessionData.get("refresh_token").getAsString() : null;
            String username = sessionData.has("username") ? sessionData.get("username").getAsString() : "Player";
            String uuid = sessionData.has("uuid") ? sessionData.get("uuid").getAsString() : "session-uuid";
            long expiresAt = sessionData.has("expires_at") ? sessionData.get("expires_at").getAsLong() : 0;

            long now = System.currentTimeMillis();
            if (now < (expiresAt - 5 * 60 * 1000) && accessToken != null && !accessToken.isEmpty()) {
                return new AuthInfos(username, accessToken, uuid);
            }

            if (refreshToken != null && !refreshToken.isEmpty()) {
                AuthInfos refreshedAuth = refreshMicrosoftToken(refreshToken, username, uuid);
                if (refreshedAuth != null) {
                    return refreshedAuth;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SESSION_FILE.delete();
        return null;
    }

    public static void saveEncryptedSession(String accessToken, String refreshToken, String username, String uuid, long expiresInSeconds) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("access_token", accessToken);
            json.addProperty("refresh_token", refreshToken);
            json.addProperty("username", username);
            json.addProperty("uuid", uuid);

            long ttl = expiresInSeconds > 0 ? expiresInSeconds : 3600;
            json.addProperty("expires_at", System.currentTimeMillis() + (ttl * 1000));

            String encryptedJson = EncryptionUtil.encrypt(json.toString());
            SESSION_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(SESSION_FILE)) {
                writer.write(encryptedJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static AuthInfos refreshMicrosoftToken(String refreshToken, String oldUsername, String oldUuid) {
        try {
            URL url = new URL("https://login.live.com/oauth20_token.srf");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String postData = "client_id=00000000402B5328" +
                    "&refresh_token=" + refreshToken +
                    "&grant_type=refresh_token" +
                    "&redirect_uri=https://login.live.com/oauth20_token.srf";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();

                    String newAccessToken = response.get("access_token").getAsString();
                    String newRefreshToken = response.has("refresh_token") ? response.get("refresh_token").getAsString() : refreshToken;
                    long expiresIn = response.has("expires_in") ? response.get("expires_in").getAsLong() : 3600;

                    saveEncryptedSession(newAccessToken, newRefreshToken, oldUsername, oldUuid, expiresIn);
                    return new AuthInfos(oldUsername, newAccessToken, oldUuid);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}

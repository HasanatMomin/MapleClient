package com.maple.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maple.launcher.services.LauncherPaths;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AuthSessionManager {
    private static final String CLIENT_ID = "000000004C12AE29";
    private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final File SESSION_FILE = new File(LauncherPaths.ROOT_DIR, "session.json");

    public static AuthInfos getValidAccessToken() throws Exception {
        if (!SESSION_FILE.exists()) return null;
        try {
            String encryptedContent = Files.readString(SESSION_FILE.toPath());
            String decryptedJson = EncryptionUtil.decrypt(encryptedContent);
            JsonObject sessionData = JsonParser.parseString(decryptedJson).getAsJsonObject();

            String refreshToken = sessionData.get("refresh_token").getAsString();
            String username = sessionData.get("username").getAsString();
            String uuid = sessionData.has("uuid") ? sessionData.get("uuid").getAsString() : "session-uuid";

            URL url = new URL("https://login.live.com/oauth20_token.srf");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String postData = "client_id=" + CLIENT_ID + "&refresh_token=" + refreshToken + "&grant_type=refresh_token&redirect_uri=" + REDIRECT_URI;
            try (OutputStream os = conn.getOutputStream()) { os.write(postData.getBytes(StandardCharsets.UTF_8)); }

            if (conn.getResponseCode() == 200) {
                JsonObject response = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                String newAccess = response.get("access_token").getAsString();
                String newRefresh = response.get("refresh_token").getAsString();
                saveEncryptedSession(username, uuid, newAccess, newRefresh);
                return new AuthInfos(username, newAccess, uuid);
            }
        } catch (Exception e) { SESSION_FILE.delete(); }
        return null;
    }

    public static void saveEncryptedSession(String user, String uuid, String access, String refresh) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("username", user);
        json.addProperty("uuid", uuid);
        json.addProperty("access_token", access);
        json.addProperty("refresh_token", refresh);
        String encryptedJson = EncryptionUtil.encrypt(json.toString());
        SESSION_FILE.getParentFile().mkdirs();
        Files.writeString(SESSION_FILE.toPath(), encryptedJson);
    }
}
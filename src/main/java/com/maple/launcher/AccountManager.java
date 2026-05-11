package com.maple.launcher;

import com.google.gson.*;
import com.maple.launcher.services.LauncherPaths;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class AccountManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public record AccountRecord(String name, String uuid, String mcToken, String role, boolean premium, Long expiresAt) {}

    public static void saveAccount(AuthInfos auth, boolean isPremium) {
        try {
            LauncherPaths.ROOT_DIR.mkdirs();
            JsonObject root = LauncherPaths.ACCOUNTS_FILE.exists()
                    ? JsonParser.parseReader(new FileReader(LauncherPaths.ACCOUNTS_FILE)).getAsJsonObject()
                    : new JsonObject();

            JsonArray accounts = root.has("accounts") ? root.getAsJsonArray("accounts") : new JsonArray();

            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.get(i).getAsJsonObject().get("name").getAsString().equalsIgnoreCase(auth.getUsername())) {
                    accounts.remove(i);
                    break;
                }
            }

            JsonObject acc = new JsonObject();
            acc.addProperty("name", auth.getUsername());
            acc.addProperty("uuid", auth.getUuid());
            acc.addProperty("token", auth.getAccessToken());
            acc.addProperty("premium", isPremium);
            acc.addProperty("role", isPremium ? "PREMIUM" : "OFFLINE");
            acc.addProperty("expiresAt", isPremium ? (System.currentTimeMillis() + 86400000L) : 0L);

            accounts.add(acc);
            root.add("accounts", accounts);
            Files.writeString(LauncherPaths.ACCOUNTS_FILE.toPath(), gson.toJson(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static AccountRecord find(String username) {
        try {
            if (!LauncherPaths.ACCOUNTS_FILE.exists()) return null;
            JsonObject root = JsonParser.parseReader(new FileReader(LauncherPaths.ACCOUNTS_FILE)).getAsJsonObject();
            if (!root.has("accounts")) return null;

            JsonArray accounts = root.getAsJsonArray("accounts");
            for (JsonElement e : accounts) {
                JsonObject o = e.getAsJsonObject();
                if (o.get("name").getAsString().equalsIgnoreCase(username)) {
                    boolean isPrem = o.get("premium").getAsBoolean();
                    return new AccountRecord(
                            o.get("name").getAsString(),
                            o.get("uuid").getAsString(),
                            o.get("token").getAsString(),
                            o.has("role") ? o.get("role").getAsString() : (isPrem ? "PREMIUM" : "OFFLINE"),
                            isPrem,
                            o.has("expiresAt") ? o.get("expiresAt").getAsLong() : 0L
                    );
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static List<AuthInfos> getSavedAccounts() {
        List<AuthInfos> list = new ArrayList<>();
        try {
            if (!LauncherPaths.ACCOUNTS_FILE.exists()) return list;
            JsonObject root = JsonParser.parseReader(new FileReader(LauncherPaths.ACCOUNTS_FILE)).getAsJsonObject();
            if (!root.has("accounts")) return list;

            JsonArray accounts = root.getAsJsonArray("accounts");
            for (JsonElement e : accounts) {
                JsonObject o = e.getAsJsonObject();
                list.add(new AuthInfos(o.get("name").getAsString(), o.get("token").getAsString(), o.get("uuid").getAsString()));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Deletes account and triggers a sync of the installations file.
     */
    public static void deleteAccount(String name) {
        try {
            if (LauncherPaths.ACCOUNTS_FILE.exists()) {
                JsonObject root = JsonParser.parseReader(new FileReader(LauncherPaths.ACCOUNTS_FILE)).getAsJsonObject();
                if (root.has("accounts")) {
                    JsonArray accounts = root.getAsJsonArray("accounts");
                    for (int i = 0; i < accounts.size(); i++) {
                        if (accounts.get(i).getAsJsonObject().get("name").getAsString().equalsIgnoreCase(name)) {
                            accounts.remove(i);
                            break;
                        }
                    }
                    Files.writeString(LauncherPaths.ACCOUNTS_FILE.toPath(), gson.toJson(root));
                }
            }
            // Force removal of "Ghost" entries from the UI/JSON
            cleanupGhostInstallations();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void cleanupGhostInstallations() {
        try {
            if (!LauncherPaths.INSTALLATIONS_FILE.exists()) return;
            JsonObject root = JsonParser.parseReader(new FileReader(LauncherPaths.INSTALLATIONS_FILE)).getAsJsonObject();
            if (!root.has("instances")) return;

            JsonObject instances = root.getAsJsonObject("instances");
            List<String> toRemove = new ArrayList<>();

            // Check every instance in JSON against the actual folder on disk
            for (String id : instances.keySet()) {
                if (!LauncherPaths.instanceDir(id).exists()) {
                    toRemove.add(id);
                }
            }

            if (!toRemove.isEmpty()) {
                for (String id : toRemove) instances.remove(id);
                Files.writeString(LauncherPaths.INSTALLATIONS_FILE.toPath(), gson.toJson(root));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
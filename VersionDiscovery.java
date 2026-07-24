package com.maple.launcher.services;

import com.google.gson.*;
import com.maple.launcher.model.InstallationType;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public final class VersionDiscovery {
    private VersionDiscovery() {}

    /** For the Installation editor "Minecraft Version" dropdown */
    public static List<String> getMinecraftVersionsOnline(InstallationType type) {
        return switch (type) {
            case VANILLA -> fetchMojangManifest("release");
            case SNAPSHOT -> fetchMojangManifest("snapshot");
            case FABRIC -> fetchFabricGameVersions();
            case LOCAL_ROAMING -> scanRoamingMinecraftVersions();
        };
    }

    /** For Local (Roaming) category only */
    public static List<String> getRoamingInstalledVersionsOnly() {
        return scanRoamingMinecraftVersions();
    }

    /** For Fabric: loader versions valid for a selected MC version */
    public static List<String> getFabricLoaderVersionsForMc(String mcVersion) {
        List<String> loaders = new ArrayList<>();
        try {
            URL url = new URL("https://meta.fabricmc.net/v2/versions/loader/" + mcVersion);
            JsonArray arr = JsonParser.parseReader(new InputStreamReader(url.openStream())).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String loader = obj.getAsJsonObject("loader").get("version").getAsString();
                loaders.add(loader);
            }
        } catch (Exception ignored) {}

        LinkedHashSet<String> set = new LinkedHashSet<>(loaders);
        loaders = new ArrayList<>(set);
        loaders.sort(Collections.reverseOrder());
        return loaders;
    }

    public static Optional<String> getLatestReleaseFromManifest() {
        try {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(
                    new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()
            )).getAsJsonObject();
            return Optional.of(root.get("latest").getAsJsonObject().get("release").getAsString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static List<String> fetchMojangManifest(String wantedType) {
        List<String> versions = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(
                    new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()
            )).getAsJsonObject();

            JsonArray arr = root.getAsJsonArray("versions");
            for (JsonElement el : arr) {
                JsonObject v = el.getAsJsonObject();
                String type = v.get("type").getAsString();
                if (!wantedType.equals(type)) continue;
                versions.add(v.get("id").getAsString());
            }
        } catch (Exception ignored) {}
        // manifest order is newest-first already
        return versions;
    }

    private static List<String> fetchFabricGameVersions() {
        List<String> versions = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseReader(new InputStreamReader(
                    new URL("https://meta.fabricmc.net/v2/versions/game").openStream()
            )).getAsJsonArray();

            for (JsonElement el : arr) {
                versions.add(el.getAsJsonObject().get("version").getAsString());
            }
        } catch (Exception ignored) {}
        // fabric API is newest-first
        return versions;
    }

    private static List<String> scanRoamingMinecraftVersions() {
        List<String> out = new ArrayList<>();
        try {
            File dir = new File(System.getenv("APPDATA"), ".minecraft\\versions");
            if (!dir.exists() || !dir.isDirectory()) return out;

            File[] children = dir.listFiles(File::isDirectory);
            if (children == null) return out;

            for (File f : children) {
                File json = new File(f, f.getName() + ".json");
                if (json.exists()) out.add(f.getName());
            }
        } catch (Exception ignored) {}

        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }
}
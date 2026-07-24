package com.maple.launcher;

import com.google.gson.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RealDownloader {
    public interface DownloadListener { void onUpdate(double progress, String speed, String fileStatus, String task); }
    private static long lastUiUpdateTime = 0;

    public static String downloadGame(String category, String version, File gameDir, DownloadListener listener) throws Exception {
        if (category.equals("Fabric")) return downloadFabric(version, gameDir, listener);
        else return downloadVanilla(version, gameDir, listener);
    }

    private static String downloadFabric(String version, File gameDir, DownloadListener listener) throws Exception {
        listener.onUpdate(0, "Fetching", "Metadata", "Fabric Setup");
        String loaderMetaRaw = readUrl("https://meta.fabricmc.net/v2/versions/loader/" + version);
        JsonArray loaderMeta = JsonParser.parseString(loaderMetaRaw).getAsJsonArray();
        String loaderVersion = loaderMeta.get(0).getAsJsonObject().getAsJsonObject("loader").get("version").getAsString();

        String profileUrl = "https://meta.fabricmc.net/v2/versions/loader/" + version + "/" + loaderVersion + "/profile/json";
        String profileRaw = readUrl(profileUrl);
        JsonObject profileJson = JsonParser.parseString(profileRaw).getAsJsonObject();
        String fabricId = "fabric-" + version;
        profileJson.addProperty("id", fabricId);

        File versionDir = new File(gameDir, "versions/" + fabricId);
        versionDir.mkdirs();
        Files.writeString(new File(versionDir, fabricId + ".json").toPath(), profileJson.toString());

        downloadVanilla(version, gameDir, listener);

        JsonArray libraries = profileJson.getAsJsonArray("libraries");
        int total = libraries.size();
        AtomicInteger current = new AtomicInteger(0);

        for (JsonElement e : libraries) {
            JsonObject lib = e.getAsJsonObject();
            String name = lib.get("name").getAsString();
            String urlBase = lib.has("url") ? lib.get("url").getAsString() : "https://maven.fabricmc.net/";
            String[] parts = name.split(":");
            String path = parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
            File target = new File(gameDir, "libraries/" + path);
            if (!target.exists()) downloadFile(urlBase + path, target, listener, current, total, "Fabric Libs");
            else throttleUpdate(listener, (double) current.incrementAndGet() / total, "Verified", current.get() + "/" + total, "Fabric Libs");
        }
        return fabricId;
    }

    private static String downloadVanilla(String version, File gameDir, DownloadListener listener) throws Exception {
        String manifestRaw = readUrl("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        JsonArray versions = JsonParser.parseString(manifestRaw).getAsJsonObject().getAsJsonArray("versions");
        String versionUrl = "";
        for (JsonElement e : versions) {
            JsonObject obj = e.getAsJsonObject();
            if (obj.get("id").getAsString().equals(version)) { versionUrl = obj.get("url").getAsString(); break; }
        }
        if (versionUrl.isEmpty()) throw new RuntimeException("Version not found: " + version);

        String versionJsonRaw = readUrl(versionUrl);
        JsonObject versionData = JsonParser.parseString(versionJsonRaw).getAsJsonObject();
        File versionDir = new File(gameDir, "versions/" + version);
        versionDir.mkdirs();
        Files.writeString(new File(versionDir, version + ".json").toPath(), versionJsonRaw);

        JsonObject assetIndexObj = versionData.getAsJsonObject("assetIndex");
        String assetIndexRaw = readUrl(assetIndexObj.get("url").getAsString());
        File assetIndexFile = new File(gameDir, "assets/indexes/" + assetIndexObj.get("id").getAsString() + ".json");
        assetIndexFile.getParentFile().mkdirs();
        Files.writeString(assetIndexFile.toPath(), assetIndexRaw);

        JsonObject objects = JsonParser.parseString(assetIndexRaw).getAsJsonObject().getAsJsonObject("objects");
        JsonArray libraries = versionData.getAsJsonArray("libraries");

        int totalFiles = libraries.size() + objects.size() + 1;
        AtomicInteger current = new AtomicInteger(0);

        for (JsonElement e : libraries) {
            JsonObject lib = e.getAsJsonObject();
            if (!lib.has("downloads")) continue;
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (!downloads.has("artifact")) continue;
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            File target = new File(gameDir, "libraries/" + artifact.get("path").getAsString());
            if (target.exists() && target.length() == artifact.get("size").getAsLong()) {
                throttleUpdate(listener, (double) current.incrementAndGet() / totalFiles, "Verified", current.get() + "/" + totalFiles, "Libraries");
                continue;
            }
            downloadFile(artifact.get("url").getAsString(), target, listener, current, totalFiles, "Libraries");
        }

        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (String key : objects.keySet()) {
            JsonObject asset = objects.getAsJsonObject(key);
            String hash = asset.get("hash").getAsString();
            String path = hash.substring(0, 2) + "/" + hash;
            File target = new File(gameDir, "assets/objects/" + path);
            executor.execute(() -> {
                if (target.exists() && target.length() == asset.get("size").getAsLong()) {
                    throttleUpdate(listener, (double) current.incrementAndGet() / totalFiles, "Verified", current.get() + "/" + totalFiles, "Assets");
                } else {
                    downloadFile("https://resources.download.minecraft.net/" + path, target, listener, current, totalFiles, "Assets");
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.HOURS);

        JsonObject clientDownload = versionData.getAsJsonObject("downloads").getAsJsonObject("client");
        File clientJar = new File(versionDir, version + ".jar");
        if (!clientJar.exists() || clientJar.length() != clientDownload.get("size").getAsLong()) {
            downloadFile(clientDownload.get("url").getAsString(), clientJar, listener, current, totalFiles, "Client");
        }
        return version;
    }

    private static void downloadFile(String url, File target, DownloadListener listener, AtomicInteger current, int total, String task) {
        try {
            target.getParentFile().mkdirs();
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    throttleUpdate(listener, (double) current.get() / total, "Downloading", current.get() + "/" + total, task);
                }
                throttleUpdate(listener, (double) current.incrementAndGet() / total, "Success", current.get() + "/" + total, task);
            }
        } catch (Exception ignored) { current.incrementAndGet(); }
    }

    private static synchronized void throttleUpdate(DownloadListener listener, double progress, String speed, String fileStatus, String task) {
        long now = System.currentTimeMillis();
        if (now - lastUiUpdateTime > 800) {
            listener.onUpdate(progress, speed, fileStatus, task);
            lastUiUpdateTime = now;
        }
    }

    private static String readUrl(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
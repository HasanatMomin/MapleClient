package com.maple.launcher;

import com.google.gson.*;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import java.io.*;
import java.util.*;

public class Launcher {
    /**
     * Starts the Minecraft process and returns it so the UI can monitor its status.
     */
    public static Process start(AuthInfos auth, String version, int ram, String javaPath, File runDir) throws Exception {
        File jsonFile = new File(runDir, "versions/" + version + "/" + version + ".json");
        if (!jsonFile.exists()) throw new FileNotFoundException("Version JSON missing in " + jsonFile.getAbsolutePath());

        JsonObject vJson = JsonParser.parseReader(new FileReader(jsonFile)).getAsJsonObject();
        String javaExe = (javaPath == null || javaPath.isEmpty()) ?
                System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe" : javaPath;

        List<String> cp = new ArrayList<>();
        String jarVersion = vJson.has("inheritsFrom") ? vJson.get("inheritsFrom").getAsString() : version;
        cp.add(new File(runDir, "versions/" + jarVersion + "/" + jarVersion + ".jar").getAbsolutePath());

        List<JsonObject> allLibraries = new ArrayList<>();
        if (vJson.has("libraries")) {
            for (JsonElement e : vJson.getAsJsonArray("libraries")) allLibraries.add(e.getAsJsonObject());
        }

        JsonObject inheritedJson = null;
        if (vJson.has("inheritsFrom")) {
            File inheritedJsonFile = new File(runDir, "versions/" + jarVersion + "/" + jarVersion + ".json");
            if (inheritedJsonFile.exists()) {
                inheritedJson = JsonParser.parseReader(new FileReader(inheritedJsonFile)).getAsJsonObject();
                if (inheritedJson.has("libraries")) {
                    for (JsonElement e : inheritedJson.getAsJsonArray("libraries")) allLibraries.add(e.getAsJsonObject());
                }
            }
        }

        for (JsonObject lib : allLibraries) {
            boolean allowed = true;
            if (lib.has("rules")) {
                allowed = false;
                for (JsonElement ruleElem : lib.getAsJsonArray("rules")) {
                    JsonObject rule = ruleElem.getAsJsonObject();
                    String action = rule.get("action").getAsString();
                    if (rule.has("os")) {
                        if (rule.getAsJsonObject("os").has("name") && rule.getAsJsonObject("os").get("name").getAsString().equals("windows")) {
                            allowed = action.equals("allow");
                        }
                    } else {
                        allowed = action.equals("allow");
                    }
                }
            }

            if (!allowed) continue;

            String path = "";
            if (lib.has("downloads") && lib.getAsJsonObject("downloads").has("artifact")) {
                path = lib.getAsJsonObject("downloads").getAsJsonObject("artifact").get("path").getAsString();
            } else if (lib.has("name")) {
                String name = lib.get("name").getAsString();
                String[] parts = name.split(":");
                path = parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
            }

            if (!path.isEmpty()) {
                File libFile = new File(runDir, "libraries/" + path);
                if (libFile.exists()) cp.add(libFile.getAbsolutePath());
            }
        }

        List<String> args = new ArrayList<>();
        args.add(javaExe);
        args.add("-Xmx" + ram + "G");
        args.add("-Djava.library.path=" + new File(runDir, "natives/" + jarVersion).getAbsolutePath());
        args.add("-cp");
        args.add(String.join(File.pathSeparator, cp));
        args.add(vJson.get("mainClass").getAsString());

        String assetIndex = jarVersion;
        if (vJson.has("assetIndex")) {
            assetIndex = vJson.getAsJsonObject("assetIndex").get("id").getAsString();
        } else if (inheritedJson != null && inheritedJson.has("assetIndex")) {
            assetIndex = inheritedJson.getAsJsonObject("assetIndex").get("id").getAsString();
        }

        args.addAll(Arrays.asList(
                "--username", auth.getUsername(),
                "--version", version,
                "--gameDir", runDir.getAbsolutePath(),
                "--assetsDir", new File(runDir, "assets").getAbsolutePath(),
                "--assetIndex", assetIndex,
                "--uuid", auth.getUuid(),
                "--accessToken", auth.getAccessToken()
        ));

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(runDir);
        pb.inheritIO();

        // Return the process so the UI can wait for it to exit
        return pb.start();
    }
}
package com.maple.launcher;

import com.google.gson.*;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class VersionManager {
    public static List<String> getVersions(String type) {
        List<String> versions = new ArrayList<>();
        try {
            if (type.equals("Fabric")) {
                URL url = new URL("https://meta.fabricmc.net/v2/versions/game");
                JsonArray array = JsonParser.parseReader(new InputStreamReader(url.openStream())).getAsJsonArray();
                for (JsonElement e : array) versions.add(e.getAsJsonObject().get("version").getAsString());
                return versions;
            } 
            URL url = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(url.openStream())).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("versions");
            for (JsonElement e : array) {
                JsonObject vObj = e.getAsJsonObject();
                String vType = vObj.get("type").getAsString();
                String vId = vObj.get("id").getAsString();
                if (type.equals("Vanilla") && vType.equals("release")) versions.add(vId);
                else if (type.equals("Snapshot") && vType.equals("snapshot")) versions.add(vId);
            }
        } catch (Exception e) {
            if (type.equals("Fabric")) return Arrays.asList("1.21.1", "1.20.4");
            return Arrays.asList("1.21.1", "1.20.4", "1.19.4");
        }
        return versions;
    }
}
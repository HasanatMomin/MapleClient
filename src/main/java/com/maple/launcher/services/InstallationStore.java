package com.maple.launcher.services;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.maple.launcher.model.Installation;
import com.maple.launcher.model.InstallationType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class InstallationStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static class State {
        public String selectedId;
        public List<Installation> installations = new ArrayList<>();
    }
    public static State load() {
        LauncherPaths.ROOT_DIR.mkdirs();
        if (!LauncherPaths.INSTALLATIONS_FILE.exists()) return createCleanState();
        try {
            String raw = Files.readString(LauncherPaths.INSTALLATIONS_FILE.toPath(), StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            State s = new State();
            s.selectedId = obj.has("selectedId") ? obj.get("selectedId").getAsString() : null;
            s.installations = GSON.fromJson(obj.get("installations"), new TypeToken<List<Installation>>(){}.getType());
            if (s.installations == null || s.installations.isEmpty()) return createCleanState();
            return s;
        } catch (Exception e) { return createCleanState(); }
    }
    public static void save(State state) {
        JsonObject obj = new JsonObject();
        obj.addProperty("selectedId", state.selectedId);
        obj.add("installations", GSON.toJsonTree(state.installations));
        try { Files.writeString(LauncherPaths.INSTALLATIONS_FILE.toPath(), GSON.toJson(obj), StandardCharsets.UTF_8); } catch (IOException ignored) {}
    }
    private static State createCleanState() {
        State s = new State();
        Installation def = createDefault();
        s.installations.add(def);
        s.selectedId = def.id;
        save(s);
        return s;
    }
    public static Installation createDefault() {
        Installation i = new Installation();
        i.id = UUID.randomUUID().toString();
        i.name = "Latest Release";
        i.type = InstallationType.VANILLA;
        i.mcVersion = "1.21.1";
        i.ramGb = 4;
        i.javaPath = "";
        i.gameDir = LauncherPaths.instanceDir(i.id).getAbsolutePath();
        i.modsDir = "";
        i.keepLauncherOpen = true;
        return i;
    }
    public static Installation duplicate(Installation src) {
        Installation i = createDefault();
        i.name = src.name + " (Copy)";
        i.type = src.type;
        i.mcVersion = src.mcVersion;
        i.ramGb = src.ramGb;
        i.javaPath = src.javaPath;
        i.gameDir = LauncherPaths.instanceDir(i.id).getAbsolutePath();
        i.modsDir = src.modsDir;
        i.keepLauncherOpen = src.keepLauncherOpen;
        return i;
    }
}
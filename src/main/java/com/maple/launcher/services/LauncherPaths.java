package com.maple.launcher.services;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LauncherPaths {

    // Base root directories
    public static final File ROOT_DIR = new File(System.getProperty("user.home"), ".mapleclient");
    public static final Path APP_DIR = ROOT_DIR.toPath();

    // Key subdirectories
    public static final File INSTANCES_DIR = new File(ROOT_DIR, "instances");
    public static final Path VERSIONS_DIR = APP_DIR.resolve("versions");

    // Key configuration files
    public static final File ACCOUNTS_FILE = new File(ROOT_DIR, "accounts.json");
    public static final File INSTALLATIONS_FILE = new File(ROOT_DIR, "installations.json");

    /**
     * Legacy getter method for directory path.
     */
    public static Path getLauncherDir() {
        return APP_DIR;
    }

    /**
     * Resolves and returns the File directory for a specific instance/installation ID.
     */
    public static File instanceDir(String id) {
        File dir = new File(INSTANCES_DIR, id);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}

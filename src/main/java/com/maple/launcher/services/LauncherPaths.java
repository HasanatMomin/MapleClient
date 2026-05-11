package com.maple.launcher.services;
import java.io.File;
public final class LauncherPaths {
    public static final File ROOT_DIR = new File(System.getenv("APPDATA"), ".mapleclient");
    public static final File INSTALLATIONS_FILE = new File(ROOT_DIR, "installations.json");
    public static final File ACCOUNTS_FILE = new File(ROOT_DIR, "accounts.json");
    public static final File INSTANCES_DIR = new File(ROOT_DIR, "instances");
    public static File instanceDir(String id) { return new File(INSTANCES_DIR, id); }
}
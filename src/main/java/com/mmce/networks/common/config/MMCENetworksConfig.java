package com.mmce.networks.common.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class MMCENetworksConfig {
    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_CLIENT = "client";
    private static final long HOT_RELOAD_CHECK_INTERVAL_MS = 5000L;

    public static int fallbackSyncIntervalTicks = 20;
    public static int transientSupplyGraceTicks = 40;
    public static int dirtyNetworkSyncIntervalTicks = 2;
    public static boolean enableSyncProfiling = false;
    public static int profilingLogIntervalTicks = 200;
    public static boolean enableFtbTeamAccess = true;
    public static double terminalTextScale = 0.65D;

    private static File configFile;
    private static Configuration configuration;
    private static long lastLoadedTimestamp = -1L;
    private static long lastReloadCheckAt;

    private MMCENetworksConfig() {
    }

    public static void load(final File file) {
        configFile = file;
        Configuration config = new Configuration(file);
        configuration = config;
        try {
            config.load();
            fallbackSyncIntervalTicks = config.getInt(
                "fallbackSyncIntervalTicks",
                CATEGORY_GENERAL,
                20,
                1,
                20 * 60,
                "Fixed fallback sync interval in ticks. Network API writes still propagate on the next world tick."
            );
            transientSupplyGraceTicks = config.getInt(
                "transientSupplyGraceTicks",
                CATEGORY_GENERAL,
                40,
                1,
                20 * 60,
                "Grace time in ticks for transient supply pulses. When a machine stops refreshing a transient supply, it remains active until this timeout expires."
            );
            dirtyNetworkSyncIntervalTicks = config.getInt(
                "dirtyNetworkSyncIntervalTicks",
                CATEGORY_GENERAL,
                2,
                1,
                20 * 60,
                "Minimum interval in ticks between dirty network controller sync passes for the same network."
            );
            enableSyncProfiling = config.getBoolean(
                "enableSyncProfiling",
                CATEGORY_GENERAL,
                false,
                "Enable lightweight sync profiling logs for debugging multi-machine network overhead."
            );
            profilingLogIntervalTicks = config.getInt(
                "profilingLogIntervalTicks",
                CATEGORY_GENERAL,
                200,
                20,
                20 * 300,
                "How often to emit profiling logs, in ticks, when sync profiling is enabled."
            );
            enableFtbTeamAccess = config.getBoolean(
                "enableFtbTeamAccess",
                CATEGORY_GENERAL,
                true,
                "Allow players to access MMCE Networks owned by FTB Utilities teammates. Requires ftbutilities and ftblib; safely ignored when they are not installed."
            );
            terminalTextScale = config.getFloat(
                "terminalTextScale",
                CATEGORY_CLIENT,
                0.65F,
                0.4F,
                1.2F,
                "Client-only scale for MMCE Networks terminal text. This is hot-reloaded while the terminal GUI is open."
            );
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
            lastLoadedTimestamp = file == null || !file.exists() ? -1L : file.lastModified();
        }
    }

    public static Configuration getConfiguration() {
        if (configuration == null) {
            configuration = new Configuration(configFile);
            configuration.load();
        }
        return configuration;
    }

    public static void saveAndReload() {
        Configuration config = getConfiguration();
        if (config.hasChanged()) {
            config.save();
        }
        if (configFile != null) {
            load(configFile);
        }
    }

    public static void reloadIfChanged() {
        if (configFile == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastReloadCheckAt < HOT_RELOAD_CHECK_INTERVAL_MS) {
            return;
        }
        lastReloadCheckAt = now;

        long timestamp = configFile.exists() ? configFile.lastModified() : -1L;
        if (timestamp != lastLoadedTimestamp) {
            load(configFile);
        }
    }
}

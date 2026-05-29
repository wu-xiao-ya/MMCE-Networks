package com.mmce.networks.common.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class MMCENetworksConfig {
    private static final String CATEGORY_GENERAL = "general";

    public static int fallbackSyncIntervalTicks = 20;
    public static int transientSupplyGraceTicks = 40;
    public static int dirtyNetworkSyncIntervalTicks = 2;
    public static boolean enableSyncProfiling = false;
    public static int profilingLogIntervalTicks = 200;

    private MMCENetworksConfig() {
    }

    public static void load(final File file) {
        Configuration config = new Configuration(file);
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
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}

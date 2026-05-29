package com.mmce.networks.common.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class MMCENetworksConfig {
    private static final String CATEGORY_GENERAL = "general";

    public static int fallbackSyncIntervalTicks = 20;
    public static int transientSupplyGraceTicks = 40;

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
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}

package com.mmce.networks.common.compat;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.UUID;

public final class FTBUtilitiesCompat {
    private static boolean searched;
    private static Method arePlayersInSameTeam;

    private FTBUtilitiesCompat() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded("ftbutilities") && Loader.isModLoaded("ftblib");
    }

    public static boolean isSameTeam(final EntityPlayerMP player, final UUID owner) {
        if (player == null || owner == null || !isLoaded() || !init()) {
            return false;
        }

        try {
            return (Boolean) arePlayersInSameTeam.invoke(null, player.getUniqueID(), owner);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean init() {
        if (!searched) {
            searched = true;
            try {
                Class<?> api = Class.forName("com.feed_the_beast.ftblib.lib.data.FTBLibAPI");
                arePlayersInSameTeam = api.getMethod("arePlayersInSameTeam", UUID.class, UUID.class);
            } catch (Exception ignored) {
                arePlayersInSameTeam = null;
            }
        }
        return arePlayersInSameTeam != null;
    }
}

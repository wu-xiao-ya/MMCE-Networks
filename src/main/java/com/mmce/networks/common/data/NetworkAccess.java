package com.mmce.networks.common.data;

import com.mmce.networks.common.compat.FTBUtilitiesCompat;
import com.mmce.networks.common.config.MMCENetworksConfig;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;
import java.util.UUID;

public final class NetworkAccess {
    private NetworkAccess() {
    }

    public static boolean canAccess(final EntityPlayerMP player, final MMCENetworkSavedData data, final int dimension, final String networkId) {
        if (player == null || data == null || networkId == null || networkId.isEmpty()) {
            return false;
        }
        return canAccess(player, data.getNetworkOwner(dimension, networkId));
    }

    public static boolean canAccess(final EntityPlayerMP player, final MMCENetworkSavedData.NetworkRef ref) {
        return ref != null && canAccess(player, ref.getOwner());
    }

    private static boolean canAccess(final EntityPlayerMP player, @Nullable final UUID owner) {
        if (owner == null) {
            return true;
        }
        if (player == null) {
            return false;
        }
        if (owner.equals(player.getUniqueID())) {
            return true;
        }
        return MMCENetworksConfig.enableFtbTeamAccess && FTBUtilitiesCompat.isSameTeam(player, owner);
    }
}

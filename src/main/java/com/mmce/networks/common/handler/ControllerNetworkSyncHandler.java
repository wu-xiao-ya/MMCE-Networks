package com.mmce.networks.common.handler;

import com.mmce.networks.common.config.MMCENetworksConfig;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.data.MMCENetworkSavedData.ControllerSnapshot;
import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.util.WorldCompat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControllerNetworkSyncHandler {
    private static final Set<DirtyNetworkKey> DIRTY_NETWORKS = new HashSet<>();

    private final MmceReflection reflection = new MmceReflection();

    public static void markNetworkDirty(final World world, final String networkId) {
        if (world == null || WorldCompat.isRemote(world) || isNullOrEmpty(networkId)) {
            return;
        }
        DIRTY_NETWORKS.add(new DirtyNetworkKey(WorldCompat.getDimension(world), networkId));
    }

    @SubscribeEvent
    public void onWorldTick(final TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || WorldCompat.isRemote(event.world) || !reflection.isAvailable()) {
            return;
        }

        int dimension = WorldCompat.getDimension(event.world);
        Set<String> dirtyNetworkIds = consumeDirtyNetworks(dimension);
        boolean fixedSync = shouldRunFixedSync(event.world);
        if (dirtyNetworkIds.isEmpty() && !fixedSync) {
            return;
        }

        MMCENetworkSavedData data = MMCENetworkSavedData.get(event.world);
        List<TileEntity> loadedTiles = WorldCompat.getLoadedTileEntities(event.world);
        for (TileEntity tile : loadedTiles) {
            if (fixedSync || shouldSyncDirtyNetwork(tile, dirtyNetworkIds)) {
                syncController(data, dimension, tile);
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (WorldCompat.isRemote(event.getWorld()) || !reflection.isAvailable()) {
            return;
        }

        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (!reflection.isControllerTile(tile)) {
            return;
        }

        MMCENetworkSavedData.get(event.getWorld()).removeControllerSnapshot(
            WorldCompat.getDimension(event.getWorld()),
            event.getPos().toLong()
        );
    }

    private void syncController(final MMCENetworkSavedData data, final int dimension, final TileEntity tile) {
        if (!reflection.isControllerTile(tile)) {
            return;
        }

        long pos = tile.getPos().toLong();
        String networkId = reflection.getBoundNetworkId(tile);
        if (isNullOrEmpty(networkId)) {
            data.removeControllerSnapshot(dimension, pos);
            return;
        }

        NBTTagCompound controllerSharedData = reflection.getSharedData(tile);
        NBTTagCompound networkSharedData;
        synchronized (data) {
            networkSharedData = data.getNetworkData(dimension, networkId);
            if (!data.hasNetwork(dimension, networkId)) {
                data.putNetworkData(dimension, networkId, networkSharedData);
            }
        }

        // The network store is the single source of truth. Never write controller-local
        // stale customData back into the network during periodic sync.
        if (!networkSharedData.equals(controllerSharedData)) {
            reflection.setSharedData(tile, networkId, networkSharedData);
            reflection.markForUpdateSync(tile);
        }
        data.putControllerSnapshot(dimension, pos, networkId, networkSharedData);
    }

    private static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    private boolean shouldSyncDirtyNetwork(final TileEntity tile, final Set<String> dirtyNetworkIds) {
        if (dirtyNetworkIds.isEmpty() || !reflection.isControllerTile(tile)) {
            return false;
        }
        String networkId = reflection.getBoundNetworkId(tile);
        return networkId != null && dirtyNetworkIds.contains(networkId);
    }

    private static boolean shouldRunFixedSync(final World world) {
        int interval = Math.max(1, MMCENetworksConfig.fallbackSyncIntervalTicks);
        return WorldCompat.getTotalWorldTime(world) % interval == 0;
    }

    private static Set<String> consumeDirtyNetworks(final int dimension) {
        Set<String> result = new HashSet<>();
        DIRTY_NETWORKS.removeIf(key -> {
            if (key.dimension != dimension) {
                return false;
            }
            result.add(key.networkId);
            return true;
        });
        return result;
    }

    private static final class DirtyNetworkKey {
        private final int dimension;
        private final String networkId;

        private DirtyNetworkKey(final int dimension, final String networkId) {
            this.dimension = dimension;
            this.networkId = networkId;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DirtyNetworkKey)) {
                return false;
            }
            DirtyNetworkKey other = (DirtyNetworkKey) obj;
            return dimension == other.dimension && networkId.equals(other.networkId);
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + networkId.hashCode();
            return result;
        }
    }
}

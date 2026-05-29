package com.mmce.networks.common.handler;

import com.mmce.networks.common.config.MMCENetworksConfig;
import com.mmce.networks.api.MMCENetworkApi;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.data.MMCENetworkSavedData.ControllerSnapshot;
import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.util.WorldCompat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControllerNetworkSyncHandler {
    private static final Object DIRTY_NETWORK_LOCK = new Object();
    private static final Set<DirtyNetworkKey> DIRTY_NETWORKS = new HashSet<>();

    private final MmceReflection reflection = new MmceReflection();

    public static void markNetworkDirty(final World world, final String networkId) {
        if (world == null || WorldCompat.isRemote(world) || isNullOrEmpty(networkId)) {
            return;
        }
        synchronized (DIRTY_NETWORK_LOCK) {
            DIRTY_NETWORKS.add(new DirtyNetworkKey(WorldCompat.getDimension(world), networkId));
        }
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
        TransientSupplyScheduler.process(event.world, dimension);
        List<Long> positions = fixedSync
            ? data.getAllControllerPositions(dimension)
            : getDirtyControllerPositions(data, dimension, dirtyNetworkIds);
        for (Long pos : positions) {
            syncController(data, dimension, event.world, pos.longValue());
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

    private void syncController(final MMCENetworkSavedData data, final int dimension, final World world, final long posLong) {
        TileEntity tile = WorldCompat.getTileEntity(world, BlockPos.fromLong(posLong));
        if (!reflection.isControllerTile(tile)) {
            data.removeControllerSnapshot(dimension, posLong);
            return;
        }

        String networkId = reflection.getBoundNetworkId(tile);
        if (isNullOrEmpty(networkId)) {
            data.removeControllerSnapshot(dimension, posLong);
            return;
        }

        NBTTagCompound controllerSharedData = reflection.getSharedData(tile);
        NBTTagCompound networkSharedData;
        synchronized (data) {
            networkSharedData = MMCENetworkApi.getSharedData(world, networkId);
            if (!data.hasNetwork(dimension, networkId)) {
                data.putNetworkData(dimension, networkId, data.getNetworkData(dimension, networkId));
            }
        }

        // The network store is the single source of truth. Never write controller-local
        // stale customData back into the network during periodic sync.
        if (!networkSharedData.equals(controllerSharedData)) {
            reflection.setSharedData(tile, networkId, networkSharedData);
            reflection.markForUpdateSync(tile);
        }
        data.putControllerSnapshot(dimension, posLong, networkId, networkSharedData);
    }

    private static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    private List<Long> getDirtyControllerPositions(final MMCENetworkSavedData data, final int dimension, final Set<String> dirtyNetworkIds) {
        if (dirtyNetworkIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        Set<Long> positions = new HashSet<>();
        for (String networkId : dirtyNetworkIds) {
            positions.addAll(data.getControllerPositions(dimension, networkId));
        }
        return new java.util.ArrayList<>(positions);
    }

    private static boolean shouldRunFixedSync(final World world) {
        int interval = Math.max(1, MMCENetworksConfig.fallbackSyncIntervalTicks);
        return WorldCompat.getTotalWorldTime(world) % interval == 0;
    }

    private static Set<String> consumeDirtyNetworks(final int dimension) {
        Set<String> result = new HashSet<>();
        synchronized (DIRTY_NETWORK_LOCK) {
            DIRTY_NETWORKS.removeIf(key -> {
                if (key.dimension != dimension) {
                    return false;
                }
                result.add(key.networkId);
                return true;
            });
        }
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

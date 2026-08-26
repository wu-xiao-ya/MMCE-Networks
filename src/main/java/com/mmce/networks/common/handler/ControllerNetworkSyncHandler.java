package com.mmce.networks.common.handler;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.config.MMCENetworksConfig;
import com.mmce.networks.api.MMCENetworkApi;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.data.MMCENetworkSavedData.ControllerSnapshot;
import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.compute.ComputeNetworkService;
import com.mmce.networks.common.compute.transport.ComputeCableNetworkService;
import com.mmce.networks.common.compute.transport.ComputeEndpointAutoBindingService;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ControllerNetworkSyncHandler {
    private static final Object DIRTY_NETWORK_LOCK = new Object();
    private static final Set<DirtyNetworkKey> DIRTY_NETWORKS = new HashSet<>();
    private static final Map<DirtyNetworkKey, Long> LAST_SYNC_TICKS = new HashMap<>();
    private static final SyncProfiler PROFILER = new SyncProfiler();

    private final MmceReflection reflection = new MmceReflection();

    public static void markNetworkDirty(final World world, final String networkId) {
        if (world == null || world.isRemote || isNullOrEmpty(networkId)) {
            return;
        }
        synchronized (DIRTY_NETWORK_LOCK) {
            DIRTY_NETWORKS.add(new DirtyNetworkKey(world.provider.getDimension(), networkId));
        }
    }

    @SubscribeEvent
    public void onWorldTick(final TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }

        int dimension = event.world.provider.getDimension();
        long worldTick = event.world.getTotalWorldTime();
        TransientSupplyScheduler.process(event.world, dimension);
        ComputeEndpointAutoBindingService.synchronize(event.world);
        ComputeNetworkService.settle(event.world);
        if (!reflection.isAvailable()) {
            return;
        }
        Set<String> dirtyNetworkIds = consumeDirtyNetworks(dimension, worldTick);
        boolean fixedSync = shouldRunFixedSync(event.world);
        if (dirtyNetworkIds.isEmpty() && !fixedSync) {
            return;
        }

        long startNanos = PROFILER.isEnabled() ? System.nanoTime() : 0L;
        MMCENetworkSavedData data = MMCENetworkSavedData.get(event.world);
        Map<String, NBTTagCompound> networkSharedDataCache = new HashMap<>();
        List<Long> positions = fixedSync
            ? data.getAllControllerPositions(dimension)
            : getDirtyControllerPositions(data, dimension, dirtyNetworkIds);
        int updatedControllers = 0;
        for (Long pos : positions) {
            if (syncController(data, dimension, event.world, pos.longValue(), networkSharedDataCache)) {
                updatedControllers++;
            }
        }
        if (PROFILER.isEnabled()) {
            PROFILER.recordPass(
                worldTick,
                dirtyNetworkIds.size(),
                positions.size(),
                updatedControllers,
                networkSharedDataCache.size(),
                System.nanoTime() - startNanos
            );
        }
    }

    @SubscribeEvent
    public void onWorldUnload(final WorldEvent.Unload event) {
        if (event.getWorld() != null && !event.getWorld().isRemote) {
            int dimension = event.getWorld().provider.getDimension();
            ComputeNetworkService.clearWorld(dimension);
            ComputeCableNetworkService.clearWorld(dimension);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote || !reflection.isAvailable()) {
            return;
        }

        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (!reflection.isControllerTile(tile)) {
            return;
        }

        String networkId = reflection.getBoundNetworkId(tile);
        if (!isNullOrEmpty(networkId)) {
            ComputeNetworkService.unbindRoute(
                event.getWorld(),
                networkId,
                event.getWorld().provider.getDimension() + ":" + event.getPos().toLong()
            );
        }
        MMCENetworkSavedData.get(event.getWorld()).removeControllerSnapshot(
            event.getWorld().provider.getDimension(),
            event.getPos().toLong()
        );
    }

    private boolean syncController(
        final MMCENetworkSavedData data,
        final int dimension,
        final World world,
        final long posLong,
        final Map<String, NBTTagCompound> networkSharedDataCache
    ) {
        TileEntity tile = world.getTileEntity(BlockPos.fromLong(posLong));
        if (!reflection.isControllerTile(tile)) {
            data.removeControllerSnapshot(dimension, posLong);
            return false;
        }

        String networkId = reflection.getBoundNetworkId(tile);
        if (isNullOrEmpty(networkId)) {
            data.removeControllerSnapshot(dimension, posLong);
            return false;
        }

        NBTTagCompound controllerSharedData = reflection.getSharedData(tile);
        NBTTagCompound networkSharedData = networkSharedDataCache.get(networkId);
        if (networkSharedData == null) {
            synchronized (data) {
                networkSharedData = MMCENetworkApi.getSharedData(world, networkId);
            }
            networkSharedDataCache.put(networkId, networkSharedData);
        }

        // Persistent network data remains the source of truth. Runtime compute
        // telemetry is appended only to the controller sync payload.
        NBTTagCompound syncPayload = networkSharedData.copy();
        ComputeNetworkService.writeSyncedTelemetry(
            world,
            networkId,
            dimension + ":" + posLong,
            syncPayload
        );
        if (!syncPayload.equals(controllerSharedData)) {
            reflection.setSharedData(tile, networkId, syncPayload);
            reflection.markForUpdateSync(tile);
            data.putControllerSnapshot(dimension, posLong, networkId, networkSharedData);
            return true;
        }
        data.putControllerSnapshot(dimension, posLong, networkId, networkSharedData);
        return false;
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
        return world.getTotalWorldTime() % interval == 0;
    }

    private static Set<String> consumeDirtyNetworks(final int dimension, final long worldTick) {
        Set<String> result = new HashSet<>();
        int minInterval = Math.max(1, MMCENetworksConfig.dirtyNetworkSyncIntervalTicks);
        synchronized (DIRTY_NETWORK_LOCK) {
            DIRTY_NETWORKS.removeIf(key -> {
                if (key.dimension != dimension) {
                    return false;
                }
                Long lastSyncTick = LAST_SYNC_TICKS.get(key);
                if (lastSyncTick != null && worldTick - lastSyncTick.longValue() < minInterval) {
                    return false;
                }
                result.add(key.networkId);
                LAST_SYNC_TICKS.put(key, worldTick);
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

    private static final class SyncProfiler {
        private long dirtyPasses;
        private long dirtyNetworks;
        private long controllersVisited;
        private long controllersUpdated;
        private long snapshotLoads;
        private long totalNanos;
        private long lastLogTick;

        private boolean isEnabled() {
            return MMCENetworksConfig.enableSyncProfiling;
        }

        private void recordPass(
            final long worldTick,
            final int dirtyNetworkCount,
            final int controllerCount,
            final int updatedControllerCount,
            final int snapshotLoadCount,
            final long elapsedNanos
        ) {
            dirtyPasses++;
            dirtyNetworks += dirtyNetworkCount;
            controllersVisited += controllerCount;
            controllersUpdated += updatedControllerCount;
            snapshotLoads += snapshotLoadCount;
            totalNanos += elapsedNanos;

            int interval = Math.max(20, MMCENetworksConfig.profilingLogIntervalTicks);
            if (worldTick - lastLogTick < interval) {
                return;
            }

            long passes = Math.max(1L, dirtyPasses);
            double avgMs = totalNanos / 1_000_000.0D / passes;
            MMCENetworksMod.LOGGER.info(
                "[mmcenetworks] sync profile: passes={}, dirtyNetworks={}, controllersVisited={}, controllersUpdated={}, snapshotLoads={}, avgPassMs={}",
                dirtyPasses,
                dirtyNetworks,
                controllersVisited,
                controllersUpdated,
                snapshotLoads,
                String.format(java.util.Locale.ROOT, "%.3f", avgMs)
            );
            dirtyPasses = 0L;
            dirtyNetworks = 0L;
            controllersVisited = 0L;
            controllersUpdated = 0L;
            snapshotLoads = 0L;
            totalNanos = 0L;
            lastLogTick = worldTick;
        }
    }
}

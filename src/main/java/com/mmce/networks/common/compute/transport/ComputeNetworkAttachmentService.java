package com.mmce.networks.common.compute.transport;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.compute.ComputeNetworkService;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.handler.ControllerNetworkSyncHandler;
import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.tile.TileComputeEndpoint;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enrolls unbound MMCE controllers into the unique compute network published
 * by a physically connected matrix. Network identity persists across outages;
 * route validation remains responsible for the current online state.
 */
public final class ComputeNetworkAttachmentService {
    private static final int SYNC_INTERVAL_TICKS = 10;
    private static final MmceReflection MMCE = new MmceReflection();

    private ComputeNetworkAttachmentService() {
    }

    public static void synchronize(final World world) {
        if (world == null || world.isRemote
            || world.getTotalWorldTime() % SYNC_INTERVAL_TICKS != 0L) {
            return;
        }
        synchronizeNow(world);
    }

    public static int synchronizeNow(final World world) {
        if (world == null || world.isRemote || !MMCE.isAutoBindingAvailable()) {
            return 0;
        }

        List<TileComputeEndpoint> endpoints =
            ComputeCableNetworkService.getRegisteredEndpoints(world);
        Map<Long, Set<String>> controllerClaims = new HashMap<>();
        Map<String, Map<BlockPos, Integer>> wirelessCoverageCache = new HashMap<>();
        List<WirelessSource> wirelessSources = new ArrayList<>();
        Set<Long> processedMatrixEndpoints = new HashSet<>();

        for (TileComputeEndpoint endpoint : endpoints) {
            if (!isMatrixRoot(world, endpoint)
                || processedMatrixEndpoints.contains(endpoint.getPos().toLong())) {
                continue;
            }

            List<TileComputeEndpoint> component =
                ComputeCableNetworkService.getPhysicalComponentEndpoints(world, endpoint.getPos());
            Set<String> rootNetworks = new HashSet<>();
            for (TileComputeEndpoint member : component) {
                if (member.getEndpointType() == ComputeEndpointType.MATRIX
                    && isMatrixRoot(world, member)) {
                    rootNetworks.add(member.getNetworkId());
                    processedMatrixEndpoints.add(member.getPos().toLong());
                }
            }
            if (rootNetworks.size() != 1) {
                continue;
            }

            String networkId = rootNetworks.iterator().next();
            for (TileComputeEndpoint member : component) {
                claimController(world, controllerClaims, member, networkId);
            }

            Map<BlockPos, Integer> wirelessCoverage = wirelessCoverageCache.computeIfAbsent(
                networkId,
                ignored -> ComputeNetworkService.getWirelessInterfaceCoverage(world, networkId)
            );
            for (TileComputeEndpoint member : component) {
                if (member.getEndpointType() != ComputeEndpointType.WIRELESS_INTERFACE
                    || !member.isBound()) {
                    continue;
                }
                Integer coverage = wirelessCoverage.get(member.getControllerPos());
                if (coverage != null && coverage.intValue() > 0) {
                    wirelessSources.add(
                        new WirelessSource(networkId, member.getPos(), coverage.intValue())
                    );
                }
            }
        }

        for (WirelessSource source : wirelessSources) {
            double maxDistanceSq = (double) source.coverage * (double) source.coverage;
            for (TileComputeEndpoint endpoint : endpoints) {
                if (endpoint.getEndpointType() != ComputeEndpointType.MACHINE_PORT
                    || !endpoint.isBound()
                    || endpoint.getControllerPos().distanceSq(source.endpointPos) > maxDistanceSq) {
                    continue;
                }
                claimController(world, controllerClaims, endpoint, source.networkId);
            }
        }

        int attached = applyClaims(world, controllerClaims);
        if (attached > 0) {
            ComputeEndpointAutoBindingService.synchronizeNow(world);
            MMCENetworksMod.LOGGER.info(
                "[mmcenetworks] physically attached {} MMCE controller(s) to compute networks in dimension {}",
                attached,
                world.provider.getDimension()
            );
        }
        return attached;
    }

    private static boolean isMatrixRoot(
        final World world,
        final TileComputeEndpoint endpoint
    ) {
        if (endpoint == null
            || endpoint.getEndpointType() != ComputeEndpointType.MATRIX
            || !endpoint.isBound()
            || !endpoint.hasNetwork()
            || endpoint.getControllerDimension() != world.provider.getDimension()
            || !world.isBlockLoaded(endpoint.getControllerPos())) {
            return false;
        }
        TileEntity controller = world.getTileEntity(endpoint.getControllerPos());
        return MMCE.isControllerTile(controller)
            && MMCE.isStructureFormed(controller)
            && endpoint.getNetworkId().equals(MMCE.getBoundNetworkId(controller));
    }

    private static void claimController(
        final World world,
        final Map<Long, Set<String>> claims,
        final TileComputeEndpoint endpoint,
        final String networkId
    ) {
        if (endpoint == null || !endpoint.isBound()
            || endpoint.getControllerDimension() != world.provider.getDimension()) {
            return;
        }
        claims.computeIfAbsent(endpoint.getControllerPos().toLong(), ignored -> new HashSet<>())
            .add(networkId);
    }

    private static int applyClaims(
        final World world,
        final Map<Long, Set<String>> claims
    ) {
        int dimension = world.provider.getDimension();
        MMCENetworkSavedData data = MMCENetworkSavedData.get(world);
        int attached = 0;
        for (Map.Entry<Long, Set<String>> entry : claims.entrySet()) {
            if (entry.getValue().size() != 1) {
                continue;
            }

            BlockPos controllerPos = BlockPos.fromLong(entry.getKey().longValue());
            if (!world.isBlockLoaded(controllerPos)) {
                continue;
            }
            TileEntity controller = world.getTileEntity(controllerPos);
            if (!MMCE.isControllerTile(controller) || !MMCE.isStructureFormed(controller)) {
                continue;
            }

            String claimedNetworkId = entry.getValue().iterator().next();
            String currentNetworkId = normalize(MMCE.getBoundNetworkId(controller));
            if (!currentNetworkId.isEmpty()) {
                continue;
            }
            if (!data.hasNetwork(dimension, claimedNetworkId)) {
                continue;
            }

            NBTTagCompound sharedData = data.getNetworkData(dimension, claimedNetworkId);
            if (!MMCE.setSharedData(controller, claimedNetworkId, sharedData)) {
                continue;
            }
            MMCE.markForUpdateSync(controller);
            data.putControllerSnapshot(
                dimension,
                controllerPos.toLong(),
                claimedNetworkId,
                sharedData
            );
            ControllerNetworkSyncHandler.markNetworkDirty(world, claimedNetworkId);
            attached++;
        }
        return attached;
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }

    private static final class WirelessSource {
        private final String networkId;
        private final BlockPos endpointPos;
        private final int coverage;

        private WirelessSource(
            final String networkId,
            final BlockPos endpointPos,
            final int coverage
        ) {
            this.networkId = networkId;
            this.endpointPos = endpointPos.toImmutable();
            this.coverage = coverage;
        }
    }
}

package com.mmce.networks.common.compute.transport;

import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.tile.TileComputeEndpoint;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keeps structure-contained compute endpoints owned by their actual MMCE controller.
 *
 * <p>The structure pattern is the source of truth. No proximity heuristic is used.</p>
 */
public final class ComputeEndpointAutoBindingService {
    private static final MmceReflection MMCE = new MmceReflection();
    private static final int SYNC_INTERVAL_TICKS = 5;

    private ComputeEndpointAutoBindingService() {
    }

    public static void synchronize(final World world) {
        if (world == null || world.isRemote || !MMCE.isAutoBindingAvailable()) {
            return;
        }
        if (world.getTotalWorldTime() % SYNC_INTERVAL_TICKS != 0L) {
            return;
        }

        int dimension = world.provider.getDimension();
        Map<Long, BindingClaim> claims = new HashMap<>();
        for (TileEntity tile : new java.util.ArrayList<>(world.loadedTileEntityList)) {
            if (!MMCE.isControllerTile(tile)) {
                continue;
            }
            String networkId = MMCE.getBoundNetworkId(tile);
            if (networkId == null || networkId.trim().isEmpty() || !MMCE.isStructureFormed(tile)) {
                continue;
            }

            Set<BlockPos> endpointPositions = MMCE.getFoundPatternPositions(tile);
            for (BlockPos relative : endpointPositions) {
                BlockPos endpointPos = tile.getPos().add(relative);
                TileEntity endpointTile = world.getTileEntity(endpointPos);
                if (!(endpointTile instanceof TileComputeEndpoint)) {
                    continue;
                }
                claims.compute(
                    endpointPos.toLong(),
                    (ignored, existing) -> existing == null
                        ? new BindingClaim(networkId, dimension, tile.getPos())
                        : existing.merge(networkId, dimension, tile.getPos())
                );
            }
        }

        for (TileComputeEndpoint endpoint : ComputeCableNetworkService.getRegisteredEndpoints(world)) {
            BindingClaim claim = claims.get(endpoint.getPos().toLong());
            if (claim == null || claim.ambiguous) {
                if (endpoint.isAutomaticBinding()) {
                    endpoint.clearAutomaticBinding();
                }
                continue;
            }
            if (!endpoint.isAutomaticBinding()
                || !endpoint.matchesBinding(claim.networkId, claim.dimension, claim.controllerPos)) {
                endpoint.bindAutomatically(claim.networkId, claim.dimension, claim.controllerPos);
            }
        }
    }

    private static final class BindingClaim {
        private final String networkId;
        private final int dimension;
        private final BlockPos controllerPos;
        private final boolean ambiguous;

        private BindingClaim(
            final String networkId,
            final int dimension,
            final BlockPos controllerPos
        ) {
            this(networkId, dimension, controllerPos, false);
        }

        private BindingClaim(
            final String networkId,
            final int dimension,
            final BlockPos controllerPos,
            final boolean ambiguous
        ) {
            this.networkId = networkId;
            this.dimension = dimension;
            this.controllerPos = controllerPos.toImmutable();
            this.ambiguous = ambiguous;
        }

        private BindingClaim merge(
            final String otherNetworkId,
            final int otherDimension,
            final BlockPos otherControllerPos
        ) {
            if (!ambiguous
                && networkId.equals(otherNetworkId)
                && dimension == otherDimension
                && controllerPos.equals(otherControllerPos)) {
                return this;
            }
            return new BindingClaim(networkId, dimension, controllerPos, true);
        }
    }
}

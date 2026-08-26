package com.mmce.networks.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Optional additional validation hook for a physical cable graph.
 *
 * <p>MMCE Networks always validates its native cable and endpoint graph first.
 * Integrations can register this hook to impose additional transport rules.</p>
 */
@FunctionalInterface
public interface ComputeWiredRouteValidator {
    boolean isConnected(
        World world,
        String networkId,
        String nodeId,
        BlockPos nodePos,
        BlockPos interfaceAnchor,
        @Nullable BlockPos distributorAnchor
    );

    /**
     * Extended hook with the matrix endpoint anchor. Existing integrations
     * remain source and binary compatible through this default bridge.
     */
    default boolean isConnected(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos,
        final BlockPos interfaceAnchor,
        @Nullable final BlockPos distributorAnchor,
        final BlockPos matrixAnchor
    ) {
        return isConnected(
            world,
            networkId,
            nodeId,
            nodePos,
            interfaceAnchor,
            distributorAnchor
        );
    }
}

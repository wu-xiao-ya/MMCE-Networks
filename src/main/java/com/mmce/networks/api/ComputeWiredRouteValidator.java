package com.mmce.networks.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Optional integration hook for a physical cable graph.
 *
 * <p>When no validator is registered, a persisted wired route is treated as
 * an explicit connection. An addon can register a validator to require an
 * actual cable path without changing MMCE Networks topology storage.</p>
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
}

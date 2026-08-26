package com.mmce.networks.api;

import com.mmce.networks.common.compute.ComputeNetworkService;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Public Java API for the real-time CU/t network.
 */
public final class ComputeNetworkApi {
    private ComputeNetworkApi() {
    }

    public static void setWiredRouteValidator(
        @Nullable final ComputeWiredRouteValidator validator
    ) {
        ComputeNetworkService.setWiredRouteValidator(validator);
    }

    public static boolean configureMatrix(
        final World world,
        final String networkId,
        final long throughput,
        final int machineLimit
    ) {
        return ComputeNetworkService.configureMatrix(world, networkId, throughput, machineLimit);
    }

    public static boolean configureMatrix(
        final World world,
        final String networkId,
        final BlockPos anchor,
        final long throughput,
        final int machineLimit
    ) {
        return ComputeNetworkService.configureMatrix(world, networkId, anchor, throughput, machineLimit);
    }

    public static boolean configureInterface(
        final World world,
        final String networkId,
        final String interfaceId,
        final long throughput,
        final int machineLimit,
        final int coverage,
        final boolean wireless
    ) {
        return ComputeNetworkService.configureInterface(
            world, networkId, interfaceId, throughput, machineLimit, coverage, wireless
        );
    }

    public static boolean configureInterface(
        final World world,
        final String networkId,
        final String interfaceId,
        final BlockPos anchor,
        final long throughput,
        final int machineLimit,
        final int coverage,
        final boolean wireless
    ) {
        return ComputeNetworkService.configureInterface(
            world, networkId, interfaceId, anchor, throughput, machineLimit, coverage, wireless
        );
    }

    public static boolean removeInterface(
        final World world,
        final String networkId,
        final String interfaceId
    ) {
        return ComputeNetworkService.removeInterface(world, networkId, interfaceId);
    }

    public static boolean configureDistributor(
        final World world,
        final String networkId,
        final String distributorId,
        final long throughput,
        final int machineLimit,
        final int bindingLimit,
        final int coverage
    ) {
        return ComputeNetworkService.configureDistributor(
            world, networkId, distributorId, throughput, machineLimit, bindingLimit, coverage
        );
    }

    public static boolean configureDistributor(
        final World world,
        final String networkId,
        final String distributorId,
        final BlockPos anchor,
        final long throughput,
        final int machineLimit,
        final int bindingLimit,
        final int coverage
    ) {
        return ComputeNetworkService.configureDistributor(
            world, networkId, distributorId, anchor, throughput, machineLimit, bindingLimit, coverage
        );
    }

    public static boolean removeDistributor(
        final World world,
        final String networkId,
        final String distributorId
    ) {
        return ComputeNetworkService.removeDistributor(world, networkId, distributorId);
    }

    public static boolean bindRoute(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos,
        final String interfaceId,
        @Nullable final String distributorId,
        @Nullable final String connectionType
    ) {
        return ComputeNetworkService.bindRoute(
            world,
            networkId,
            nodeId,
            nodePos,
            interfaceId,
            distributorId,
            connectionType
        );
    }

    public static boolean unbindRoute(
        final World world,
        final String networkId,
        final String nodeId
    ) {
        return ComputeNetworkService.unbindRoute(world, networkId, nodeId);
    }

    public static boolean hasRoute(
        final World world,
        final String networkId,
        final String nodeId
    ) {
        return ComputeNetworkService.hasRoute(world, networkId, nodeId);
    }

    public static String getRouteStatus(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos
    ) {
        return ComputeNetworkService.getRouteStatus(world, networkId, nodeId, nodePos);
    }

    public static boolean reportNode(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos,
        final long cpuOutput,
        final long demand
    ) {
        return ComputeNetworkService.reportNode(
            world, networkId, nodeId, nodePos, cpuOutput, demand
        );
    }

    /**
     * Legacy overload. The supplied path must match the bound server route.
     */
    public static boolean reportNode(
        final World world,
        final String networkId,
        final String nodeId,
        @Nullable final String interfaceId,
        @Nullable final String distributorId,
        final long cpuOutput,
        final long demand
    ) {
        return ComputeNetworkService.reportNode(
            world, networkId, nodeId, interfaceId, distributorId, cpuOutput, demand
        );
    }

    public static long getAllocated(
        final World world,
        final String networkId,
        final String nodeId
    ) {
        return ComputeNetworkService.getAllocated(world, networkId, nodeId);
    }

    public static boolean isDemandSatisfied(
        final World world,
        final String networkId,
        final String nodeId
    ) {
        return ComputeNetworkService.isDemandSatisfied(world, networkId, nodeId);
    }

    public static long getTotalCpuOutput(final World world, final String networkId) {
        return ComputeNetworkService.getTotalCpuOutput(world, networkId);
    }

    public static long getTotalDemand(final World world, final String networkId) {
        return ComputeNetworkService.getTotalDemand(world, networkId);
    }

    public static long getTotalAllocated(final World world, final String networkId) {
        return ComputeNetworkService.getTotalAllocated(world, networkId);
    }

    public static int getEligibleNodes(final World world, final String networkId) {
        return ComputeNetworkService.getEligibleNodes(world, networkId);
    }

    public static int getRejectedNodes(final World world, final String networkId) {
        return ComputeNetworkService.getRejectedNodes(world, networkId);
    }

    public static NBTTagCompound getSnapshot(final World world, final String networkId) {
        return ComputeNetworkService.getSnapshot(world, networkId);
    }
}

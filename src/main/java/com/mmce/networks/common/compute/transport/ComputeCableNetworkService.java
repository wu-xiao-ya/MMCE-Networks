package com.mmce.networks.common.compute.transport;

import com.mmce.networks.common.block.BlockComputeCable;
import com.mmce.networks.common.block.BlockComputeEndpoint;
import com.mmce.networks.common.config.MMCENetworksConfig;
import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.tile.TileComputeEndpoint;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ComputeCableNetworkService {
    private static final Object LOCK = new Object();
    private static final Map<Integer, Set<Long>> ENDPOINTS = new HashMap<>();
    private static final Map<Integer, Long> REVISIONS = new HashMap<>();
    private static final Map<PathKey, Boolean> PATH_CACHE = new HashMap<>();
    private static final MmceReflection MMCE = new MmceReflection();

    private ComputeCableNetworkService() {
    }

    public static void registerEndpoint(final TileComputeEndpoint endpoint) {
        if (!validEndpoint(endpoint)) {
            return;
        }
        int dimension = endpoint.getWorld().provider.getDimension();
        synchronized (LOCK) {
            ENDPOINTS.computeIfAbsent(dimension, ignored -> new HashSet<>())
                .add(endpoint.getPos().toLong());
            dirtyDimension(dimension);
        }
    }

    public static void unregisterEndpoint(final TileComputeEndpoint endpoint) {
        if (endpoint == null || endpoint.getWorld() == null || endpoint.getWorld().isRemote) {
            return;
        }
        int dimension = endpoint.getWorld().provider.getDimension();
        synchronized (LOCK) {
            Set<Long> positions = ENDPOINTS.get(dimension);
            if (positions != null) {
                positions.remove(endpoint.getPos().toLong());
                if (positions.isEmpty()) {
                    ENDPOINTS.remove(dimension);
                }
            }
            dirtyDimension(dimension);
        }
    }

    public static List<TileComputeEndpoint> getRegisteredEndpoints(final World world) {
        List<TileComputeEndpoint> result = new ArrayList<>();
        if (world == null || world.isRemote) {
            return result;
        }

        int dimension = world.provider.getDimension();
        List<Long> positions;
        synchronized (LOCK) {
            Set<Long> indexed = ENDPOINTS.get(dimension);
            positions = indexed == null
                ? java.util.Collections.emptyList()
                : new ArrayList<>(indexed);
        }
        for (Long position : positions) {
            BlockPos endpointPos = BlockPos.fromLong(position.longValue());
            if (!world.isBlockLoaded(endpointPos)) {
                continue;
            }
            TileEntity tile = world.getTileEntity(endpointPos);
            if (tile instanceof TileComputeEndpoint && !tile.isInvalid()) {
                result.add((TileComputeEndpoint) tile);
            }
        }
        return result;
    }

    public static List<TileComputeEndpoint> getPhysicalComponentEndpoints(
        final World world,
        final BlockPos start
    ) {
        List<TileComputeEndpoint> result = new ArrayList<>();
        if (world == null || world.isRemote || start == null || !isPhysicalTraversable(world, start)) {
            return result;
        }

        int maxVisited = Math.max(64, MMCENetworksConfig.computeCableMaxVisitedNodes);
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(start.toImmutable());
        visited.add(start.toLong());

        while (!queue.isEmpty() && visited.size() <= maxVisited) {
            BlockPos current = queue.removeFirst();
            TileEntity tile = world.getTileEntity(current);
            if (tile instanceof TileComputeEndpoint && !tile.isInvalid()) {
                result.add((TileComputeEndpoint) tile);
            }
            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos next = current.offset(facing);
                if (!visited.add(next.toLong()) || !isPhysicalTraversable(world, next)) {
                    continue;
                }
                queue.addLast(next.toImmutable());
            }
        }
        return result;
    }

    public static void markGraphDirty(final World world) {
        if (world == null || world.isRemote) {
            return;
        }
        synchronized (LOCK) {
            dirtyDimension(world.provider.getDimension());
        }
    }

    public static void clearWorld(final int dimension) {
        synchronized (LOCK) {
            ENDPOINTS.remove(dimension);
            REVISIONS.remove(dimension);
            PATH_CACHE.keySet().removeIf(key -> key.dimension == dimension);
        }
    }

    public static ValidationResult validateWiredRoute(
        final World world,
        final String networkId,
        final BlockPos nodeController,
        final BlockPos interfaceController,
        @Nullable final BlockPos distributorController,
        final BlockPos matrixController
    ) {
        List<BlockPos> machinePorts = findEndpoints(
            world, networkId, nodeController, ComputeEndpointType.MACHINE_PORT
        );
        if (machinePorts.isEmpty()) {
            return ValidationResult.MACHINE_ENDPOINT_MISSING;
        }
        List<BlockPos> interfaces = findEndpoints(
            world, networkId, interfaceController, ComputeEndpointType.WIRED_INTERFACE
        );
        if (interfaces.isEmpty()) {
            return ValidationResult.INTERFACE_ENDPOINT_MISSING;
        }
        List<BlockPos> distributors = distributorController == null
            ? java.util.Collections.emptyList()
            : findEndpoints(world, networkId, distributorController, ComputeEndpointType.DISTRIBUTOR);
        if (distributorController != null && distributors.isEmpty()) {
            return ValidationResult.DISTRIBUTOR_ENDPOINT_MISSING;
        }
        List<BlockPos> matrices = findEndpoints(
            world, networkId, matrixController, ComputeEndpointType.MATRIX
        );
        if (matrices.isEmpty()) {
            return ValidationResult.MATRIX_ENDPOINT_MISSING;
        }

        for (BlockPos machinePort : machinePorts) {
            for (BlockPos computeInterface : interfaces) {
                if (!isConnected(world, networkId, machinePort, computeInterface)) {
                    continue;
                }
                if (distributorController == null) {
                    if (anyConnected(world, networkId, computeInterface, matrices)) {
                        return ValidationResult.VALID;
                    }
                    continue;
                }
                for (BlockPos distributor : distributors) {
                    for (BlockPos matrix : matrices) {
                        if (isConnectedThroughDistributor(
                            world,
                            networkId,
                            computeInterface,
                            distributor,
                            matrix
                        )) {
                            return ValidationResult.VALID;
                        }
                    }
                }
            }
        }
        return ValidationResult.WIRED_PATH_INVALID;
    }

    public static ValidationResult validateWirelessRoute(
        final World world,
        final String networkId,
        final BlockPos nodeController,
        final BlockPos interfaceController,
        @Nullable final BlockPos distributorController,
        final BlockPos matrixController,
        final int coverage
    ) {
        List<BlockPos> machinePorts = findEndpoints(
            world, networkId, nodeController, ComputeEndpointType.MACHINE_PORT
        );
        if (machinePorts.isEmpty()) {
            return ValidationResult.MACHINE_ENDPOINT_MISSING;
        }
        List<BlockPos> interfaces = findEndpoints(
            world, networkId, interfaceController, ComputeEndpointType.WIRELESS_INTERFACE
        );
        if (interfaces.isEmpty()) {
            return ValidationResult.INTERFACE_ENDPOINT_MISSING;
        }
        List<BlockPos> inRangeInterfaces = new ArrayList<>();
        double range = Math.max(0, coverage);
        double maxDistanceSq = range * range;
        for (BlockPos computeInterface : interfaces) {
            if (nodeController.distanceSq(computeInterface) <= maxDistanceSq) {
                inRangeInterfaces.add(computeInterface);
            }
        }
        if (inRangeInterfaces.isEmpty()) {
            return ValidationResult.OUT_OF_RANGE;
        }

        List<BlockPos> distributors = distributorController == null
            ? java.util.Collections.emptyList()
            : findEndpoints(world, networkId, distributorController, ComputeEndpointType.DISTRIBUTOR);
        if (distributorController != null && distributors.isEmpty()) {
            return ValidationResult.DISTRIBUTOR_ENDPOINT_MISSING;
        }
        List<BlockPos> matrices = findEndpoints(
            world, networkId, matrixController, ComputeEndpointType.MATRIX
        );
        if (matrices.isEmpty()) {
            return ValidationResult.MATRIX_ENDPOINT_MISSING;
        }

        for (BlockPos computeInterface : inRangeInterfaces) {
            if (distributorController == null) {
                if (anyConnected(world, networkId, computeInterface, matrices)) {
                    return ValidationResult.VALID;
                }
                continue;
            }
            for (BlockPos distributor : distributors) {
                for (BlockPos matrix : matrices) {
                    if (isConnectedThroughDistributor(
                        world,
                        networkId,
                        computeInterface,
                        distributor,
                        matrix
                    )) {
                        return ValidationResult.VALID;
                    }
                }
            }
        }
        return ValidationResult.WIRELESS_BACKBONE_INVALID;
    }

    public static boolean isConnected(
        final World world,
        final String networkId,
        final BlockPos start,
        final BlockPos target
    ) {
        if (world == null || world.isRemote || networkId == null || networkId.isEmpty()
            || start == null || target == null) {
            return false;
        }
        if (start.equals(target)) {
            return true;
        }
        int dimension = world.provider.getDimension();
        long revision;
        synchronized (LOCK) {
            revision = REVISIONS.getOrDefault(dimension, 0L);
        }
        PathKey key = new PathKey(dimension, networkId, start, target, revision);
        synchronized (LOCK) {
            Boolean cached = PATH_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        boolean connected = search(world, networkId, start, target);
        synchronized (LOCK) {
            PATH_CACHE.put(key, connected);
        }
        return connected;
    }

    private static boolean isConnectedThroughDistributor(
        final World world,
        final String networkId,
        final BlockPos start,
        final BlockPos distributor,
        final BlockPos target
    ) {
        if (start == null || distributor == null || target == null
            || start.equals(distributor) || distributor.equals(target)) {
            return false;
        }
        if (!isConnected(world, networkId, start, distributor)
            || !isConnected(world, networkId, distributor, target)) {
            return false;
        }
        /*
         * A distributor is a logical exchange point and throughput limiter.
         * Redundant cable routes are valid; requiring the distributor to be
         * the graph's unique cut vertex makes normal looped layouts fail even
         * though every declared segment is physically connected.
         */
        return true;
    }

    private static boolean search(
        final World world,
        final String networkId,
        final BlockPos start,
        final BlockPos target
    ) {
        return search(world, networkId, start, target, null);
    }

    private static boolean search(
        final World world,
        final String networkId,
        final BlockPos start,
        final BlockPos target,
        @Nullable final BlockPos blocked
    ) {
        if (blocked != null && (blocked.equals(start) || blocked.equals(target))) {
            return false;
        }
        if (!isTraversable(world, networkId, start) || !isTraversable(world, networkId, target)) {
            return false;
        }
        int maxVisited = Math.max(64, MMCENetworksConfig.computeCableMaxVisitedNodes);
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(start.toImmutable());
        visited.add(start.toLong());

        while (!queue.isEmpty() && visited.size() <= maxVisited) {
            BlockPos current = queue.removeFirst();
            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos next = current.offset(facing);
                if ((blocked != null && blocked.equals(next)) || !visited.add(next.toLong())) {
                    continue;
                }
                if (!isTraversable(world, networkId, next)) {
                    continue;
                }
                if (next.equals(target)) {
                    return true;
                }
                queue.addLast(next.toImmutable());
            }
        }
        return false;
    }

    private static boolean isTraversable(
        final World world,
        final String networkId,
        final BlockPos pos
    ) {
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BlockComputeCable) {
            return true;
        }
        if (!(block instanceof BlockComputeEndpoint)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileComputeEndpoint
            && networkId.equals(((TileComputeEndpoint) tile).getNetworkId());
    }

    private static boolean isPhysicalTraversable(
        final World world,
        final BlockPos pos
    ) {
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        Block block = world.getBlockState(pos).getBlock();
        return block instanceof BlockComputeCable || block instanceof BlockComputeEndpoint;
    }

    private static boolean anyConnected(
        final World world,
        final String networkId,
        final BlockPos start,
        final List<BlockPos> targets
    ) {
        for (BlockPos target : targets) {
            if (isConnected(world, networkId, start, target)) {
                return true;
            }
        }
        return false;
    }

    private static List<BlockPos> findEndpoints(
        final World world,
        final String networkId,
        final BlockPos controllerPos,
        final ComputeEndpointType endpointType
    ) {
        List<BlockPos> result = new ArrayList<>();
        if (world == null || world.isRemote || networkId == null || networkId.isEmpty()
            || controllerPos == null || endpointType == null) {
            return result;
        }
        int dimension = world.provider.getDimension();
        List<Long> positions;
        synchronized (LOCK) {
            Set<Long> indexed = ENDPOINTS.get(dimension);
            positions = indexed == null
                ? java.util.Collections.emptyList()
                : new ArrayList<>(indexed);
        }
        if (!isControllerBindingValid(world, networkId, controllerPos)) {
            return result;
        }
        for (Long position : positions) {
            BlockPos endpointPos = BlockPos.fromLong(position.longValue());
            if (!world.isBlockLoaded(endpointPos)) {
                continue;
            }
            TileEntity tile = world.getTileEntity(endpointPos);
            if (!(tile instanceof TileComputeEndpoint)) {
                continue;
            }
            TileComputeEndpoint endpoint = (TileComputeEndpoint) tile;
            if (endpoint.getEndpointType() == endpointType
                && endpoint.matchesBinding(networkId, dimension, controllerPos)) {
                result.add(endpointPos);
            }
        }
        return result;
    }

    private static boolean isControllerBindingValid(
        final World world,
        final String networkId,
        final BlockPos controllerPos
    ) {
        if (!world.isBlockLoaded(controllerPos)) {
            return false;
        }
        TileEntity controller = world.getTileEntity(controllerPos);
        return MMCE.isControllerTile(controller)
            && MMCE.isStructureFormed(controller)
            && networkId.equals(MMCE.getBoundNetworkId(controller));
    }

    private static boolean validEndpoint(final TileComputeEndpoint endpoint) {
        return endpoint != null
            && endpoint.getWorld() != null
            && !endpoint.getWorld().isRemote
            && !endpoint.isInvalid();
    }

    private static void dirtyDimension(final int dimension) {
        REVISIONS.put(dimension, REVISIONS.getOrDefault(dimension, 0L) + 1L);
        PATH_CACHE.keySet().removeIf(key -> key.dimension == dimension);
    }

    public enum ValidationResult {
        VALID,
        MACHINE_ENDPOINT_MISSING,
        INTERFACE_ENDPOINT_MISSING,
        DISTRIBUTOR_ENDPOINT_MISSING,
        MATRIX_ENDPOINT_MISSING,
        WIRED_PATH_INVALID,
        WIRELESS_BACKBONE_INVALID,
        OUT_OF_RANGE
    }

    private static final class PathKey {
        private final int dimension;
        private final String networkId;
        private final long first;
        private final long second;
        private final long revision;

        private PathKey(
            final int dimension,
            final String networkId,
            final BlockPos start,
            final BlockPos target,
            final long revision
        ) {
            this.dimension = dimension;
            this.networkId = networkId;
            long startPos = start.toLong();
            long targetPos = target.toLong();
            this.first = Math.min(startPos, targetPos);
            this.second = Math.max(startPos, targetPos);
            this.revision = revision;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PathKey)) {
                return false;
            }
            PathKey other = (PathKey) obj;
            return dimension == other.dimension
                && first == other.first
                && second == other.second
                && revision == other.revision
                && networkId.equals(other.networkId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, networkId, first, second, revision);
        }
    }
}

package com.mmce.networks.common.compute;

import com.mmce.networks.api.ComputeWiredRouteValidator;
import com.mmce.networks.common.compute.transport.ComputeCableNetworkService;
import com.mmce.networks.common.compute.transport.ComputeCableNetworkService.ValidationResult;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.handler.ControllerNetworkSyncHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Per-tick compute routing. Topology is persisted in the network shared NBT;
 * node reports and allocations are intentionally ephemeral.
 */
public final class ComputeNetworkService {
    private static final String ROOT_TAG = "_computeNetwork";
    private static final int SCHEMA_VERSION = 2;
    private static final String MATRIX_TAG = "matrix";
    private static final String INTERFACES_TAG = "interfaces";
    private static final String DISTRIBUTORS_TAG = "distributors";
    private static final String ROUTES_TAG = "routes";
    private static final String THROUGHPUT_TAG = "throughput";
    private static final String MACHINE_LIMIT_TAG = "machineLimit";
    private static final String COVERAGE_TAG = "coverage";
    private static final String WIRELESS_TAG = "wireless";
    private static final String BINDING_LIMIT_TAG = "bindingLimit";
    private static final String DIMENSION_TAG = "dimension";
    private static final String POSITION_TAG = "position";
    private static final String INTERFACE_ID_TAG = "interfaceId";
    private static final String DISTRIBUTOR_ID_TAG = "distributorId";
    private static final String CONNECTION_TYPE_TAG = "connectionType";
    private static final String ENABLED_TAG = "enabled";
    private static final String TELEMETRY_TAG = "_computeTelemetry";

    private static final long UNLIMITED = Long.MAX_VALUE;
    private static final int DEFAULT_COVERAGE = 0;
    public static final String CONNECTION_WIRED = "wired";
    public static final String CONNECTION_WIRELESS = "wireless";

    private static final Object LOCK = new Object();
    private static final Map<NetworkKey, RuntimeNetwork> RUNTIME = new HashMap<>();
    @Nullable
    private static volatile ComputeWiredRouteValidator wiredRouteValidator;

    private ComputeNetworkService() {
    }

    public static void setWiredRouteValidator(
        @Nullable final ComputeWiredRouteValidator validator
    ) {
        wiredRouteValidator = validator;
    }

    public static boolean configureMatrix(
        final World world,
        final String networkId,
        final long throughput,
        final int machineLimit
    ) {
        return configureMatrix(world, networkId, null, throughput, machineLimit);
    }

    public static boolean configureMatrix(
        final World world,
        final String networkId,
        @Nullable final BlockPos anchor,
        final long throughput,
        final int machineLimit
    ) {
        if (!validWorld(world) || empty(networkId) || throughput < 0L || machineLimit < 0) {
            return false;
        }
        return mutateTopology(world, networkId, data -> {
            NBTTagCompound root = getRoot(data, true);
            NBTTagCompound matrix = getCompound(root, MATRIX_TAG, true);
            matrix.setLong(THROUGHPUT_TAG, throughput);
            matrix.setInteger(MACHINE_LIMIT_TAG, machineLimit);
            writeAnchor(matrix, world, anchor);
            root.setInteger("schema", SCHEMA_VERSION);
            return true;
        });
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
        return configureInterface(
            world, networkId, interfaceId, null, throughput, machineLimit, coverage, wireless
        );
    }

    public static boolean configureInterface(
        final World world,
        final String networkId,
        final String interfaceId,
        @Nullable final BlockPos anchor,
        final long throughput,
        final int machineLimit,
        final int coverage,
        final boolean wireless
    ) {
        if (!validWorld(world) || empty(networkId) || empty(interfaceId)
            || throughput < 0L || machineLimit < 0 || coverage < 0) {
            return false;
        }
        return mutateTopology(world, networkId, data -> {
            NBTTagCompound root = getRoot(data, true);
            NBTTagCompound interfaces = getCompound(root, INTERFACES_TAG, true);
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong(THROUGHPUT_TAG, throughput);
            entry.setInteger(MACHINE_LIMIT_TAG, machineLimit);
            entry.setInteger(COVERAGE_TAG, coverage);
            entry.setBoolean(WIRELESS_TAG, wireless);
            writeAnchor(entry, world, anchor);
            interfaces.setTag(interfaceId, entry);
            root.setInteger("schema", SCHEMA_VERSION);
            return true;
        });
    }

    public static boolean removeInterface(final World world, final String networkId, final String interfaceId) {
        return removeTopologyEntry(world, networkId, INTERFACES_TAG, interfaceId);
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
        return configureDistributor(
            world, networkId, distributorId, null, throughput, machineLimit, bindingLimit, coverage
        );
    }

    public static boolean configureDistributor(
        final World world,
        final String networkId,
        final String distributorId,
        @Nullable final BlockPos anchor,
        final long throughput,
        final int machineLimit,
        final int bindingLimit,
        final int coverage
    ) {
        if (!validWorld(world) || empty(networkId) || empty(distributorId)
            || throughput < 0L || machineLimit < 0 || bindingLimit < 0 || coverage < 0) {
            return false;
        }
        return mutateTopology(world, networkId, data -> {
            NBTTagCompound root = getRoot(data, true);
            NBTTagCompound distributors = getCompound(root, DISTRIBUTORS_TAG, true);
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong(THROUGHPUT_TAG, throughput);
            entry.setInteger(MACHINE_LIMIT_TAG, machineLimit);
            entry.setInteger(BINDING_LIMIT_TAG, bindingLimit);
            entry.setInteger(COVERAGE_TAG, coverage);
            writeAnchor(entry, world, anchor);
            distributors.setTag(distributorId, entry);
            root.setInteger("schema", SCHEMA_VERSION);
            return true;
        });
    }

    public static boolean removeDistributor(final World world, final String networkId, final String distributorId) {
        return removeTopologyEntry(world, networkId, DISTRIBUTORS_TAG, distributorId);
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
        if (!validWorld(world) || empty(networkId) || empty(nodeId) || nodePos == null || empty(interfaceId)) {
            return false;
        }
        String normalizedType = normalizeConnectionType(connectionType);
        if (normalizedType == null) {
            return false;
        }
        return mutateTopology(world, networkId, data -> {
            NBTTagCompound root = getRoot(data, true);
            NBTTagCompound routes = getCompound(root, ROUTES_TAG, true);
            NBTTagCompound interfaces = getCompound(root, INTERFACES_TAG, false);
            NBTTagCompound line = interfaces == null
                ? null
                : getCompound(interfaces, normalize(interfaceId), false);
            if (line == null
                || line.getBoolean(WIRELESS_TAG) != CONNECTION_WIRELESS.equals(normalizedType)) {
                return false;
            }
            String normalizedDistributor = normalize(distributorId);
            NBTTagCompound distributors = getCompound(root, DISTRIBUTORS_TAG, false);
            NBTTagCompound distributor = distributors == null
                ? null
                : getCompound(distributors, normalizedDistributor, false);
            if (!normalizedDistributor.isEmpty() && distributor == null) {
                return false;
            }
            int bindingLimit = distributor == null
                ? 0
                : Math.max(0, distributor.getInteger(BINDING_LIMIT_TAG));
            if (bindingLimit > 0
                && countRoutesForDistributor(routes, normalizedDistributor, nodeId) >= bindingLimit) {
                return false;
            }
            NBTTagCompound route = new NBTTagCompound();
            route.setString(INTERFACE_ID_TAG, normalize(interfaceId));
            route.setString(DISTRIBUTOR_ID_TAG, normalizedDistributor);
            route.setString(CONNECTION_TYPE_TAG, normalizedType);
            route.setBoolean(ENABLED_TAG, true);
            route.setInteger(DIMENSION_TAG, world.provider.getDimension());
            route.setLong(POSITION_TAG, nodePos.toLong());
            routes.setTag(nodeId, route);
            root.setInteger("schema", SCHEMA_VERSION);
            return true;
        });
    }

    public static boolean unbindRoute(
        final World world,
        final String networkId,
        final String nodeId
    ) {
        return removeTopologyEntry(world, networkId, ROUTES_TAG, nodeId);
    }

    public static boolean hasRoute(
        final World world,
        final String networkId,
        final String nodeId
    ) {
        if (!validWorld(world) || empty(networkId) || empty(nodeId)) {
            return false;
        }
        Topology topology = readTopology(world, networkId);
        return topology.routes.containsKey(nodeId);
    }

    public static String getRouteStatus(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos
    ) {
        if (!validWorld(world) || empty(networkId) || empty(nodeId) || nodePos == null) {
            return RouteStatus.INVALID_CONTEXT.id;
        }
        return resolveRoute(
            world, networkId, Topology.read(getNetworkData(world, networkId)), nodeId, nodePos
        ).status.id;
    }

    public static Map<BlockPos, Integer> getWirelessInterfaceCoverage(
        final World world,
        final String networkId
    ) {
        Map<BlockPos, Integer> result = new HashMap<>();
        if (!validWorld(world) || empty(networkId)) {
            return result;
        }
        Topology topology = readTopology(world, networkId);
        int dimension = world.provider.getDimension();
        for (InterfaceConfig config : topology.interfaces.values()) {
            if (!config.wireless || config.coverage <= 0 || config.anchor == null
                || config.anchor.dimension != dimension) {
                continue;
            }
            result.merge(config.anchor.position, config.coverage, Math::max);
        }
        return result;
    }

    public static boolean reportNode(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos,
        final long cpuOutput,
        final long demand
    ) {
        if (!validWorld(world) || empty(networkId) || empty(nodeId)
            || nodePos == null || cpuOutput < 0L || demand < 0L) {
            return false;
        }
        Topology topology = Topology.read(getNetworkData(world, networkId));
        RouteResolution resolution = resolveRoute(world, networkId, topology, nodeId, nodePos);
        if (!resolution.isValid()) {
            rejectNodeReport(world, networkId, nodeId, nodePos, null, resolution.status);
            return false;
        }
        return recordNodeReport(
            world,
            networkId,
            nodeId,
            nodePos,
            resolution.route.interfaceId,
            resolution.route.distributorId,
            null,
            cpuOutput,
            demand,
            resolution.status
        );
    }

    /**
     * Reports one contribution from a machine thread. The machine remains a
     * single topology node, while allocation can be resolved per contribution.
     */
    public static boolean reportNodeContribution(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos,
        @Nullable final String contributionId,
        final long cpuOutput,
        final long demand
    ) {
        if (!validWorld(world) || empty(networkId) || empty(nodeId)
            || nodePos == null || cpuOutput < 0L || demand < 0L) {
            return false;
        }
        Topology topology = Topology.read(getNetworkData(world, networkId));
        RouteResolution resolution = resolveRoute(world, networkId, topology, nodeId, nodePos);
        if (!resolution.isValid()) {
            rejectNodeReport(
                world, networkId, nodeId, nodePos, contributionId, resolution.status
            );
            return false;
        }
        return recordNodeReport(
            world,
            networkId,
            nodeId,
            nodePos,
            resolution.route.interfaceId,
            resolution.route.distributorId,
            contributionId,
            cpuOutput,
            demand,
            resolution.status
        );
    }

    /**
     * Legacy compatibility entry point. The supplied path is accepted only
     * when it exactly matches the server-side route bound to this node.
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
        if (!validWorld(world) || empty(networkId) || empty(nodeId) || cpuOutput < 0L || demand < 0L) {
            return false;
        }
        BlockPos nodePos = parseNodePos(world, nodeId);
        if (nodePos == null) {
            return false;
        }
        Topology topology = Topology.read(getNetworkData(world, networkId));
        RouteResolution resolution = resolveRoute(world, networkId, topology, nodeId, nodePos);
        if (!resolution.isValid()
            || !resolution.route.interfaceId.equals(normalize(interfaceId))
            || !resolution.route.distributorId.equals(normalize(distributorId))) {
            rejectNodeReport(world, networkId, nodeId, nodePos, null, resolution.status);
            return false;
        }
        return recordNodeReport(
            world,
            networkId,
            nodeId,
            nodePos,
            resolution.route.interfaceId,
            resolution.route.distributorId,
            null,
            cpuOutput,
            demand,
            resolution.status
        );
    }

    private static boolean recordNodeReport(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos,
        final String interfaceId,
        final String distributorId,
        @Nullable final String contributionId,
        final long cpuOutput,
        final long demand,
        final RouteStatus routeStatus
    ) {
        NetworkKey key = new NetworkKey(world.provider.getDimension(), networkId);
        synchronized (LOCK) {
            RuntimeNetwork runtime = RUNTIME.computeIfAbsent(key, ignored -> new RuntimeNetwork());
            String normalizedInterface = normalize(interfaceId);
            String normalizedDistributor = normalize(distributorId);
            long tick = world.getTotalWorldTime();
            if (tick <= runtime.lastSettledTick) {
                return false;
            }
            Map<String, NodeReport> reports = runtime.reportsByTick.computeIfAbsent(
                tick,
                ignored -> new HashMap<>()
            );
            NodeReport report = reports.computeIfAbsent(nodeId, ignored -> new NodeReport());
            String normalizedContribution = normalizeContributionId(contributionId);
            if (report.tick == tick) {
                if (!report.interfaceId.equals(normalizedInterface)
                    || !report.distributorId.equals(normalizedDistributor)
                    || !report.nodePos.equals(nodePos)) {
                    return false;
                }
                report.cpuOutput = safeAdd(report.cpuOutput, cpuOutput);
                report.demand = safeAdd(report.demand, demand);
            } else {
                report.interfaceId = normalizedInterface;
                report.distributorId = normalizedDistributor;
                report.nodePos = nodePos.toImmutable();
                report.cpuOutput = cpuOutput;
                report.demand = demand;
                report.tick = tick;
            }
            Contribution contribution = report.contributions.computeIfAbsent(
                normalizedContribution,
                ignored -> new Contribution()
            );
            contribution.cpuOutput = safeAdd(contribution.cpuOutput, cpuOutput);
            contribution.demand = safeAdd(contribution.demand, demand);
            report.routeStatus = routeStatus;
        }
        return true;
    }

    private static void rejectNodeReport(
        final World world,
        final String networkId,
        final String nodeId,
        final BlockPos nodePos,
        @Nullable final String contributionId,
        final RouteStatus routeStatus
    ) {
        NetworkKey key = new NetworkKey(world.provider.getDimension(), networkId);
        synchronized (LOCK) {
            RuntimeNetwork runtime = RUNTIME.computeIfAbsent(key, ignored -> new RuntimeNetwork());
            long tick = world.getTotalWorldTime();
            if (tick <= runtime.lastSettledTick) {
                return;
            }
            Map<String, NodeReport> reports = runtime.reportsByTick.computeIfAbsent(
                tick,
                ignored -> new HashMap<>()
            );
            NodeReport report = reports.computeIfAbsent(nodeId, ignored -> new NodeReport());
            report.interfaceId = "";
            report.distributorId = "";
            report.nodePos = nodePos.toImmutable();
            report.cpuOutput = 0L;
            report.demand = 0L;
            report.tick = tick;
            report.routeStatus = routeStatus;
            report.contributions.clear();
            report.contributions.put(normalizeContributionId(contributionId), new Contribution());
        }
    }

    public static void settle(final World world) {
        if (!validWorld(world)) {
            return;
        }

        int dimension = world.provider.getDimension();
        // Reports can arrive asynchronously during this END phase. Settle only
        // the completed tick so the current tick stays open for late reports.
        long tick = world.getTotalWorldTime() - 1L;
        if (tick < 0L) {
            return;
        }
        List<NetworkKey> keys;
        synchronized (LOCK) {
            keys = new ArrayList<>();
            for (Map.Entry<NetworkKey, RuntimeNetwork> entry : RUNTIME.entrySet()) {
                if (entry.getKey().dimension == dimension) {
                    keys.add(entry.getKey());
                }
            }
        }

        for (NetworkKey key : keys) {
            settleNetwork(world, key, tick);
        }
    }

    public static long getAllocated(
        final World world,
        final String networkId,
        final String nodeId
    ) {
        if (!validWorld(world) || empty(networkId) || empty(nodeId)) {
            return 0L;
        }
        synchronized (LOCK) {
            RuntimeNetwork runtime = RUNTIME.get(new NetworkKey(world.provider.getDimension(), networkId));
            NodeResult result = runtime == null ? null : runtime.results.get(nodeId);
            return result == null ? 0L : result.allocated;
        }
    }

    public static long getAllocated(
        final World world,
        final String networkId,
        final String nodeId,
        @Nullable final String contributionId
    ) {
        if (!validWorld(world) || empty(networkId) || empty(nodeId)) {
            return 0L;
        }
        synchronized (LOCK) {
            RuntimeNetwork runtime = RUNTIME.get(new NetworkKey(world.provider.getDimension(), networkId));
            NodeResult result = runtime == null ? null : runtime.results.get(nodeId);
            if (result == null) {
                return 0L;
            }
            return result.getContributionAllocated(normalizeContributionId(contributionId));
        }
    }

    public static boolean isDemandSatisfied(
        final World world,
        final String networkId,
        final String nodeId
    ) {
        if (!validWorld(world) || empty(networkId) || empty(nodeId)) {
            return false;
        }
        synchronized (LOCK) {
            RuntimeNetwork runtime = RUNTIME.get(new NetworkKey(world.provider.getDimension(), networkId));
            NodeResult result = runtime == null ? null : runtime.results.get(nodeId);
            return result != null && result.demand > 0L && result.allocated >= result.demand;
        }
    }

    public static boolean isDemandSatisfied(
        final World world,
        final String networkId,
        final String nodeId,
        @Nullable final String contributionId
    ) {
        if (!validWorld(world) || empty(networkId) || empty(nodeId)) {
            return false;
        }
        synchronized (LOCK) {
            RuntimeNetwork runtime = RUNTIME.get(new NetworkKey(world.provider.getDimension(), networkId));
            NodeResult result = runtime == null ? null : runtime.results.get(nodeId);
            return result != null
                && result.isContributionSatisfied(normalizeContributionId(contributionId));
        }
    }

    public static long getTotalCpuOutput(final World world, final String networkId) {
        RuntimeNetwork runtime = getRuntime(world, networkId);
        return runtime == null ? 0L : runtime.totalCpuOutput;
    }

    public static long getTotalDemand(final World world, final String networkId) {
        RuntimeNetwork runtime = getRuntime(world, networkId);
        return runtime == null ? 0L : runtime.totalDemand;
    }

    public static long getTotalAllocated(final World world, final String networkId) {
        RuntimeNetwork runtime = getRuntime(world, networkId);
        return runtime == null ? 0L : runtime.totalAllocated;
    }

    public static int getEligibleNodes(final World world, final String networkId) {
        RuntimeNetwork runtime = getRuntime(world, networkId);
        return runtime == null ? 0 : runtime.eligibleNodes;
    }

    public static int getRejectedNodes(final World world, final String networkId) {
        RuntimeNetwork runtime = getRuntime(world, networkId);
        return runtime == null ? 0 : runtime.rejectedNodes;
    }

    public static NBTTagCompound getSnapshot(final World world, final String networkId) {
        if (!validWorld(world) || empty(networkId)) {
            return new NBTTagCompound();
        }

        NBTTagCompound snapshot = new NBTTagCompound();
        NBTTagCompound data = MMCENetworkSavedData.get(world).getNetworkData(
            world.provider.getDimension(), networkId
        );
        NBTTagCompound root = getRoot(data, false);
        if (root != null) {
            snapshot.setTag("topology", root.copy());
        }

        RuntimeNetwork runtime;
        synchronized (LOCK) {
            runtime = RUNTIME.get(new NetworkKey(world.provider.getDimension(), networkId));
            if (runtime == null) {
                return snapshot;
            }
            snapshot.setLong("lastTick", runtime.lastSettledTick);
            snapshot.setLong("cpuOutput", runtime.totalCpuOutput);
            snapshot.setLong("demand", runtime.totalDemand);
            snapshot.setLong("allocated", runtime.totalAllocated);
            snapshot.setInteger("eligibleNodes", runtime.eligibleNodes);
            snapshot.setInteger("rejectedNodes", runtime.rejectedNodes);

            NBTTagCompound nodes = new NBTTagCompound();
            for (Map.Entry<String, NodeResult> entry : runtime.results.entrySet()) {
                NBTTagCompound node = new NBTTagCompound();
                node.setLong("demand", entry.getValue().demand);
                node.setLong("allocated", entry.getValue().allocated);
                node.setBoolean("satisfied", entry.getValue().allocated >= entry.getValue().demand
                    && entry.getValue().demand > 0L);
                node.setString("routeStatus", entry.getValue().routeStatus.id);
                nodes.setTag(entry.getKey(), node);
            }
            snapshot.setTag("nodes", nodes);
        }
        return snapshot;
    }

    public static void writeSyncedTelemetry(
        final World world,
        final String networkId,
        final String nodeId,
        final NBTTagCompound target
    ) {
        if (target == null) {
            return;
        }

        NBTTagCompound telemetry = new NBTTagCompound();
        if (validWorld(world) && !empty(networkId)) {
            BlockPos nodePos = parseNodePos(world, nodeId);
            if (nodePos != null) {
                telemetry.setString(
                    "nodeRouteStatus",
                    resolveRoute(
                        world,
                        networkId,
                        Topology.read(target),
                        nodeId,
                        nodePos
                    ).status.id
                );
            }
            synchronized (LOCK) {
                RuntimeNetwork runtime = RUNTIME.get(new NetworkKey(world.provider.getDimension(), networkId));
                if (runtime != null) {
                    telemetry.setLong("lastTick", runtime.lastSettledTick);
                    telemetry.setLong("cpuOutput", runtime.totalCpuOutput);
                    telemetry.setLong("demand", runtime.totalDemand);
                    telemetry.setLong("allocated", runtime.totalAllocated);
                    telemetry.setInteger("eligibleNodes", runtime.eligibleNodes);
                    telemetry.setInteger("rejectedNodes", runtime.rejectedNodes);
                    NodeResult result = runtime.results.get(nodeId);
                    if (result != null) {
                        telemetry.setLong("nodeDemand", result.demand);
                        telemetry.setLong("nodeAllocated", result.allocated);
                        telemetry.setBoolean(
                            "nodeSatisfied",
                            result.demand > 0L && result.allocated >= result.demand
                        );
                        telemetry.setString("nodeRouteStatus", result.routeStatus.id);
                    }
                }
            }
        }
        target.setTag(TELEMETRY_TAG, telemetry);
    }

    public static NBTTagCompound getSyncedTelemetry(final NBTTagCompound sharedData) {
        if (sharedData == null || !sharedData.hasKey(TELEMETRY_TAG, Constants.NBT.TAG_COMPOUND)) {
            return new NBTTagCompound();
        }
        return sharedData.getCompoundTag(TELEMETRY_TAG).copy();
    }

    @Nullable
    private static RuntimeNetwork getRuntime(final World world, final String networkId) {
        if (!validWorld(world) || empty(networkId)) {
            return null;
        }
        synchronized (LOCK) {
            return RUNTIME.get(new NetworkKey(world.provider.getDimension(), networkId));
        }
    }

    public static void clearWorld(final int dimension) {
        synchronized (LOCK) {
            RUNTIME.keySet().removeIf(key -> key.dimension == dimension);
        }
    }

    private static void settleNetwork(final World world, final NetworkKey key, final long tick) {
        RuntimeNetwork runtime;
        Map<String, NodeReport> reports;
        Map<String, NodeResult> previousResults;
        long previousCpuOutput;
        long previousDemand;
        long previousAllocated;
        int previousEligibleNodes;
        int previousRejectedNodes;
        synchronized (LOCK) {
            runtime = RUNTIME.get(key);
            if (runtime == null) {
                return;
            }
            if (tick <= runtime.lastSettledTick) {
                return;
            }
            previousResults = new HashMap<>(runtime.results);
            previousCpuOutput = runtime.totalCpuOutput;
            previousDemand = runtime.totalDemand;
            previousAllocated = runtime.totalAllocated;
            previousEligibleNodes = runtime.eligibleNodes;
            previousRejectedNodes = runtime.rejectedNodes;
            runtime.results.clear();
            runtime.totalCpuOutput = 0L;
            runtime.totalDemand = 0L;
            runtime.totalAllocated = 0L;
            runtime.eligibleNodes = 0;
            runtime.rejectedNodes = 0;
            // Detach the completed bucket while holding the same lock used by
            // reporters; newer buckets remain available to the next settle.
            reports = runtime.reportsByTick.remove(tick);
            runtime.reportsByTick.entrySet().removeIf(entry -> entry.getKey() <= tick);
            runtime.lastSettledTick = tick;
        }

        NBTTagCompound data = getNetworkData(world, key.networkId);
        Topology topology = Topology.read(data);
        List<NodeEntry> entries = new ArrayList<>();
        if (reports != null) {
            for (Map.Entry<String, NodeReport> entry : reports.entrySet()) {
                entries.add(new NodeEntry(entry.getKey(), entry.getValue()));
            }
        }
        entries.sort(Comparator.comparing(node -> node.nodeId));

        Set<String> accepted = selectEligibleNodes(world, key.networkId, entries, topology);
        long totalCpu = 0L;
        long totalDemand = 0L;
        for (NodeEntry entry : entries) {
            if (!accepted.contains(entry.nodeId)) {
                continue;
            }
            NodeReport report = entry.report;
            totalCpu = safeAdd(totalCpu, report.cpuOutput);
            totalDemand = safeAdd(totalDemand, report.demand);
        }

        Map<String, Long> remainingInterfaceSupply = new HashMap<>();
        Map<String, Long> remainingDistributorSupply = new HashMap<>();
        long usableSupply = 0L;
        for (NodeEntry entry : entries) {
            if (!accepted.contains(entry.nodeId) || entry.report.cpuOutput <= 0L) {
                continue;
            }
            long amount = entry.report.cpuOutput;
            amount = Math.min(amount, getRemainingInterfaceCapacity(
                remainingInterfaceSupply, topology, entry.report.interfaceId
            ));
            amount = Math.min(amount, getRemainingDistributorCapacity(
                remainingDistributorSupply, topology, entry.report.distributorId
            ));
            if (amount <= 0L) {
                continue;
            }
            consumeCapacity(remainingInterfaceSupply, topology.interfaces, entry.report.interfaceId, amount);
            consumeDistributorCapacity(
                remainingDistributorSupply, topology.distributors, entry.report.distributorId, amount
            );
            usableSupply = safeAdd(usableSupply, amount);
        }
        usableSupply = Math.min(usableSupply, topology.matrixThroughput);

        long remaining = usableSupply;
        Map<String, Long> remainingInterfaceDemand = new HashMap<>();
        Map<String, Long> remainingDistributorDemand = new HashMap<>();
        Map<String, Long> allocatedByNode = new HashMap<>();
        Map<String, Map<String, Long>> allocatedByContribution = new HashMap<>();
        for (NodeEntry entry : entries) {
            NodeReport report = entry.report;
            if (!accepted.contains(entry.nodeId) || report.demand <= 0L) {
                continue;
            }
            for (ContributionEntry contributionEntry : getContributions(report)) {
                Contribution contribution = contributionEntry.contribution;
                if (contribution.demand <= 0L) {
                    continue;
                }
                long demandPathCapacity = Math.min(
                    getRemainingInterfaceCapacity(
                        remainingInterfaceDemand, topology, report.interfaceId
                    ),
                    getRemainingDistributorCapacity(
                        remainingDistributorDemand, topology, report.distributorId
                    )
                );
                long allocated = Math.min(contribution.demand, Math.min(remaining, demandPathCapacity));
                if (allocated > 0L) {
                    consumeCapacity(
                        remainingInterfaceDemand, topology.interfaces, report.interfaceId, allocated
                    );
                    consumeDistributorCapacity(
                        remainingDistributorDemand,
                        topology.distributors,
                        report.distributorId,
                        allocated
                    );
                    remaining -= allocated;
                    allocatedByNode.put(
                        entry.nodeId,
                        safeAdd(allocatedByNode.getOrDefault(entry.nodeId, 0L), allocated)
                    );
                    Map<String, Long> contributionAllocations = allocatedByContribution.computeIfAbsent(
                        entry.nodeId,
                        ignored -> new HashMap<>()
                    );
                    contributionAllocations.put(
                        contributionEntry.id,
                        safeAdd(
                            contributionAllocations.getOrDefault(contributionEntry.id, 0L),
                            allocated
                        )
                    );
                }
            }
        }

        boolean telemetryChanged;
        synchronized (LOCK) {
            for (NodeEntry entry : entries) {
                Map<String, Long> contributionAllocations = allocatedByContribution.get(entry.nodeId);
                Map<String, ContributionResult> contributionResults = new HashMap<>();
                for (ContributionEntry contributionEntry : getContributions(entry.report)) {
                    long allocated = contributionAllocations == null
                        ? 0L
                        : contributionAllocations.getOrDefault(contributionEntry.id, 0L);
                    contributionResults.put(
                        contributionEntry.id,
                        new ContributionResult(contributionEntry.contribution.demand, allocated)
                    );
                }
                runtime.results.put(
                    entry.nodeId,
                    new NodeResult(
                        entry.report.demand,
                        accepted.contains(entry.nodeId)
                            ? allocatedByNode.getOrDefault(entry.nodeId, 0L)
                            : 0L,
                        entry.report.routeStatus,
                        contributionResults
                    )
                );
            }
            runtime.lastSettledTick = tick;
            runtime.totalCpuOutput = totalCpu;
            runtime.totalDemand = totalDemand;
            runtime.totalAllocated = usableSupply - remaining;
            runtime.eligibleNodes = accepted.size();
            runtime.rejectedNodes = Math.max(0, entries.size() - accepted.size());
            telemetryChanged = previousCpuOutput != runtime.totalCpuOutput
                || previousDemand != runtime.totalDemand
                || previousAllocated != runtime.totalAllocated
                || previousEligibleNodes != runtime.eligibleNodes
                || previousRejectedNodes != runtime.rejectedNodes
                || !previousResults.equals(runtime.results);
        }
        if (telemetryChanged) {
            ControllerNetworkSyncHandler.markNetworkDirty(world, key.networkId);
        }
    }

    private static Set<String> selectEligibleNodes(
        final World world,
        final String networkId,
        final List<NodeEntry> entries,
        final Topology topology
    ) {
        Set<String> accepted = new HashSet<>();
        Map<String, Integer> lineCounts = new HashMap<>();
        Map<String, Integer> distributorCounts = new HashMap<>();
        int matrixCount = 0;
        for (NodeEntry entry : entries) {
            NodeReport report = entry.report;
            RouteResolution resolution = resolveRoute(
                world, networkId, topology, entry.nodeId, report.nodePos
            );
            report.routeStatus = resolution.status;
            if (!resolution.isValid()) {
                continue;
            }
            report.interfaceId = resolution.route.interfaceId;
            report.distributorId = resolution.route.distributorId;
            InterfaceConfig line = topology.interfaces.get(report.interfaceId);
            DistributorConfig distributor = topology.distributors.get(report.distributorId);
            if (line == null) {
                continue;
            }
            if (!report.distributorId.isEmpty() && distributor == null) {
                continue;
            }
            String lineKey = report.interfaceId;
            String distributorKey = report.distributorId;
            int lineCount = lineCounts.getOrDefault(lineKey, 0);
            int distributorCount = distributorCounts.getOrDefault(distributorKey, 0);
            if (line != null && line.machineLimit > 0 && lineCount >= line.machineLimit) {
                continue;
            }
            if (distributor != null && distributor.machineLimit > 0 && distributorCount >= distributor.machineLimit) {
                continue;
            }
            if (topology.matrixMachineLimit > 0 && matrixCount >= topology.matrixMachineLimit) {
                continue;
            }
            if (distributor != null && distributor.bindingLimit > 0 && distributorCount >= distributor.bindingLimit) {
                continue;
            }
            accepted.add(entry.nodeId);
            lineCounts.put(lineKey, lineCount + 1);
            distributorCounts.put(distributorKey, distributorCount + 1);
            matrixCount++;
        }
        return accepted;
    }

    private static List<ContributionEntry> getContributions(final NodeReport report) {
        List<ContributionEntry> result = new ArrayList<>();
        for (Map.Entry<String, Contribution> entry : report.contributions.entrySet()) {
            result.add(new ContributionEntry(entry.getKey(), entry.getValue()));
        }
        result.sort(Comparator.comparing(entry -> entry.id));
        return result;
    }

    private static long getRemainingInterfaceCapacity(
        final Map<String, Long> remaining,
        final Topology topology,
        final String interfaceId
    ) {
        if (remaining.containsKey(interfaceId)) {
            return remaining.get(interfaceId);
        }
        InterfaceConfig config = topology.interfaces.get(interfaceId);
        long capacity = config == null ? UNLIMITED : config.throughput;
        remaining.put(interfaceId, capacity);
        return capacity;
    }

    private static long getRemainingDistributorCapacity(
        final Map<String, Long> remaining,
        final Topology topology,
        final String distributorId
    ) {
        if (remaining.containsKey(distributorId)) {
            return remaining.get(distributorId);
        }
        DistributorConfig config = topology.distributors.get(distributorId);
        long capacity = config == null ? UNLIMITED : config.throughput;
        remaining.put(distributorId, capacity);
        return capacity;
    }

    private static void consumeCapacity(
        final Map<String, Long> remaining,
        final Map<String, InterfaceConfig> configs,
        final String id,
        final long amount
    ) {
        if (!configs.containsKey(id)) {
            return;
        }
        remaining.put(id, Math.max(0L, remaining.getOrDefault(id, UNLIMITED) - amount));
    }

    private static void consumeDistributorCapacity(
        final Map<String, Long> remaining,
        final Map<String, DistributorConfig> configs,
        final String id,
        final long amount
    ) {
        if (!configs.containsKey(id)) {
            return;
        }
        remaining.put(id, Math.max(0L, remaining.getOrDefault(id, UNLIMITED) - amount));
    }

    private static Topology readTopology(final World world, final String networkId) {
        return Topology.read(getNetworkData(world, networkId));
    }

    private static NBTTagCompound getNetworkData(final World world, final String networkId) {
        return MMCENetworkSavedData.get(world).getNetworkData(
            world.provider.getDimension(), networkId
        );
    }

    private static void writeAnchor(
        final NBTTagCompound target,
        final World world,
        @Nullable final BlockPos anchor
    ) {
        if (anchor == null) {
            target.removeTag(DIMENSION_TAG);
            target.removeTag(POSITION_TAG);
            return;
        }
        target.setInteger(DIMENSION_TAG, world.provider.getDimension());
        target.setLong(POSITION_TAG, anchor.toLong());
    }

    @Nullable
    private static String normalizeConnectionType(@Nullable final String connectionType) {
        String normalized = normalize(connectionType).toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            return CONNECTION_WIRED;
        }
        return CONNECTION_WIRED.equals(normalized) || CONNECTION_WIRELESS.equals(normalized)
            ? normalized
            : null;
    }

    @Nullable
    private static BlockPos parseNodePos(final World world, final String nodeId) {
        int separator = nodeId.indexOf(':');
        if (separator <= 0 || separator >= nodeId.length() - 1) {
            return null;
        }
        try {
            int dimension = Integer.parseInt(nodeId.substring(0, separator));
            if (dimension != world.provider.getDimension()) {
                return null;
            }
            return BlockPos.fromLong(Long.parseLong(nodeId.substring(separator + 1)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int countRoutesForDistributor(
        final NBTTagCompound routes,
        final String distributorId,
        final String excludedNodeId
    ) {
        if (routes == null || empty(distributorId)) {
            return 0;
        }
        int count = 0;
        for (String nodeId : routes.getKeySet()) {
            if (nodeId.equals(excludedNodeId)) {
                continue;
            }
            NBTTagCompound route = getCompound(routes, nodeId, false);
            if (route != null
                && route.getBoolean(ENABLED_TAG)
                && distributorId.equals(route.getString(DISTRIBUTOR_ID_TAG))) {
                count++;
            }
        }
        return count;
    }

    private static RouteResolution resolveRoute(
        final World world,
        final String networkId,
        final Topology topology,
        final String nodeId,
        @Nullable final BlockPos nodePos
    ) {
        if (world == null || nodePos == null) {
            return RouteResolution.invalid(RouteStatus.INVALID_CONTEXT);
        }
        RouteConfig route = topology.routes.get(nodeId);
        if (route == null) {
            return RouteResolution.invalid(RouteStatus.UNBOUND);
        }
        if (!route.enabled) {
            return RouteResolution.of(route, RouteStatus.DISABLED);
        }
        int dimension = world.provider.getDimension();
        if (route.dimension != dimension || !route.position.equals(nodePos)) {
            return RouteResolution.of(route, RouteStatus.NODE_MOVED);
        }
        InterfaceConfig line = topology.interfaces.get(route.interfaceId);
        if (line == null) {
            return RouteResolution.of(route, RouteStatus.INTERFACE_MISSING);
        }
        DistributorConfig distributor = null;
        if (!route.distributorId.isEmpty()) {
            distributor = topology.distributors.get(route.distributorId);
            if (distributor == null) {
                return RouteResolution.of(route, RouteStatus.DISTRIBUTOR_MISSING);
            }
        }
        boolean routeWireless = CONNECTION_WIRELESS.equals(route.connectionType);
        if (line.wireless != routeWireless) {
            return RouteResolution.of(route, RouteStatus.CONNECTION_TYPE_MISMATCH);
        }
        if (line.anchor == null) {
            return RouteResolution.of(route, RouteStatus.INTERFACE_ANCHOR_MISSING);
        }
        if (line.anchor.dimension != dimension) {
            return RouteResolution.of(route, RouteStatus.DIMENSION_MISMATCH);
        }
        if (topology.matrixAnchor == null) {
            return RouteResolution.of(route, RouteStatus.MATRIX_ANCHOR_MISSING);
        }
        if (topology.matrixAnchor.dimension != dimension) {
            return RouteResolution.of(route, RouteStatus.DIMENSION_MISMATCH);
        }
        BlockPos distributorAnchor = null;
        if (distributor != null) {
            if (distributor.anchor == null) {
                return RouteResolution.of(route, RouteStatus.DISTRIBUTOR_ANCHOR_MISSING);
            }
            if (distributor.anchor.dimension != dimension) {
                return RouteResolution.of(route, RouteStatus.DIMENSION_MISMATCH);
            }
            distributorAnchor = distributor.anchor.position;
        }

        ValidationResult physicalResult = routeWireless
            ? ComputeCableNetworkService.validateWirelessRoute(
                world,
                networkId,
                nodePos,
                line.anchor.position,
                distributorAnchor,
                topology.matrixAnchor.position,
                line.coverage
            )
            : ComputeCableNetworkService.validateWiredRoute(
                world,
                networkId,
                nodePos,
                line.anchor.position,
                distributorAnchor,
                topology.matrixAnchor.position
            );
        RouteStatus physicalStatus = mapPhysicalStatus(physicalResult);
        if (physicalStatus != RouteStatus.VALID) {
            return RouteResolution.of(route, physicalStatus);
        }

        if (!routeWireless) {
            ComputeWiredRouteValidator validator = wiredRouteValidator;
            if (validator != null) {
                boolean connected;
                try {
                    connected = validator.isConnected(
                        world,
                        networkId,
                        nodeId,
                        nodePos,
                        line.anchor.position,
                        distributorAnchor,
                        topology.matrixAnchor.position
                    );
                } catch (RuntimeException ignored) {
                    connected = false;
                }
                if (!connected) {
                    return RouteResolution.of(route, RouteStatus.WIRED_PATH_INVALID);
                }
            }
        }
        return RouteResolution.of(route, RouteStatus.VALID);
    }

    private static RouteStatus mapPhysicalStatus(final ValidationResult result) {
        if (result == null) {
            return RouteStatus.WIRED_PATH_INVALID;
        }
        switch (result) {
            case VALID:
                return RouteStatus.VALID;
            case MACHINE_ENDPOINT_MISSING:
                return RouteStatus.MACHINE_ENDPOINT_MISSING;
            case INTERFACE_ENDPOINT_MISSING:
                return RouteStatus.INTERFACE_ENDPOINT_MISSING;
            case DISTRIBUTOR_ENDPOINT_MISSING:
                return RouteStatus.DISTRIBUTOR_ENDPOINT_MISSING;
            case MATRIX_ENDPOINT_MISSING:
                return RouteStatus.MATRIX_ENDPOINT_MISSING;
            case WIRELESS_BACKBONE_INVALID:
                return RouteStatus.WIRELESS_BACKBONE_INVALID;
            case OUT_OF_RANGE:
                return RouteStatus.OUT_OF_RANGE;
            case WIRED_PATH_INVALID:
            default:
                return RouteStatus.WIRED_PATH_INVALID;
        }
    }

    private static boolean removeTopologyEntry(
        final World world,
        final String networkId,
        final String section,
        final String entryId
    ) {
        if (!validWorld(world) || empty(networkId) || empty(entryId)) {
            return false;
        }
        return mutateTopology(world, networkId, data -> {
            NBTTagCompound root = getRoot(data, false);
            NBTTagCompound entries = root == null ? null : getCompound(root, section, false);
            if (entries == null || !entries.hasKey(entryId, Constants.NBT.TAG_COMPOUND)) {
                return false;
            }
            entries.removeTag(entryId);
            return true;
        });
    }

    private static boolean mutateTopology(
        final World world,
        final String networkId,
        final TopologyMutation mutation
    ) {
        MMCENetworkSavedData savedData = MMCENetworkSavedData.get(world);
        synchronized (savedData) {
            NBTTagCompound data = savedData.getNetworkDataMutable(
                world.provider.getDimension(), networkId
            );
            boolean changed = mutation.apply(data);
            if (changed) {
                savedData.markDirty();
                ControllerNetworkSyncHandler.markNetworkDirty(world, networkId);
            }
            return changed;
        }
    }

    @Nullable
    private static NBTTagCompound getRoot(final NBTTagCompound data, final boolean create) {
        if (data == null) {
            return null;
        }
        if (data.hasKey(ROOT_TAG, Constants.NBT.TAG_COMPOUND)) {
            return data.getCompoundTag(ROOT_TAG);
        }
        if (!create) {
            return null;
        }
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("schema", SCHEMA_VERSION);
        data.setTag(ROOT_TAG, root);
        return root;
    }

    @Nullable
    private static NBTTagCompound getCompound(
        final NBTTagCompound parent,
        final String key,
        final boolean create
    ) {
        if (parent == null) {
            return null;
        }
        if (parent.hasKey(key, Constants.NBT.TAG_COMPOUND)) {
            return parent.getCompoundTag(key);
        }
        if (!create) {
            return null;
        }
        NBTTagCompound value = new NBTTagCompound();
        parent.setTag(key, value);
        return value;
    }

    private static boolean validWorld(@Nullable final World world) {
        return world != null && !world.isRemote;
    }

    private static boolean empty(@Nullable final String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalize(@Nullable final String value) {
        return empty(value) ? "" : value.trim();
    }

    private static String normalizeContributionId(@Nullable final String value) {
        return normalize(value);
    }

    private static long safeAdd(final long left, final long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static final class Topology {
        private long matrixThroughput = UNLIMITED;
        private int matrixMachineLimit;
        @Nullable
        private Anchor matrixAnchor;
        private final Map<String, InterfaceConfig> interfaces = new HashMap<>();
        private final Map<String, DistributorConfig> distributors = new HashMap<>();
        private final Map<String, RouteConfig> routes = new HashMap<>();

        private static Topology read(final NBTTagCompound data) {
            Topology topology = new Topology();
            NBTTagCompound root = getRoot(data, false);
            if (root == null) {
                return topology;
            }
            NBTTagCompound matrix = getCompound(root, MATRIX_TAG, false);
            if (matrix != null) {
                topology.matrixThroughput = Math.max(0L, matrix.getLong(THROUGHPUT_TAG));
                topology.matrixMachineLimit = Math.max(0, matrix.getInteger(MACHINE_LIMIT_TAG));
                topology.matrixAnchor = Anchor.read(matrix);
            }
            NBTTagCompound interfaces = getCompound(root, INTERFACES_TAG, false);
            if (interfaces != null) {
                for (String id : interfaces.getKeySet()) {
                    NBTTagCompound entry = getCompound(interfaces, id, false);
                    if (entry != null) {
                        topology.interfaces.put(id, InterfaceConfig.read(entry));
                    }
                }
            }
            NBTTagCompound distributors = getCompound(root, DISTRIBUTORS_TAG, false);
            if (distributors != null) {
                for (String id : distributors.getKeySet()) {
                    NBTTagCompound entry = getCompound(distributors, id, false);
                    if (entry != null) {
                        topology.distributors.put(id, DistributorConfig.read(entry));
                    }
                }
            }
            NBTTagCompound routes = getCompound(root, ROUTES_TAG, false);
            if (routes != null) {
                for (String nodeId : routes.getKeySet()) {
                    NBTTagCompound entry = getCompound(routes, nodeId, false);
                    if (entry != null) {
                        topology.routes.put(nodeId, RouteConfig.read(entry));
                    }
                }
            }
            return topology;
        }
    }

    private static final class InterfaceConfig {
        private long throughput = UNLIMITED;
        private int machineLimit;
        private int coverage = DEFAULT_COVERAGE;
        private boolean wireless;
        @Nullable
        private Anchor anchor;

        private static InterfaceConfig read(final NBTTagCompound entry) {
            InterfaceConfig value = new InterfaceConfig();
            value.throughput = Math.max(0L, entry.getLong(THROUGHPUT_TAG));
            value.machineLimit = Math.max(0, entry.getInteger(MACHINE_LIMIT_TAG));
            value.coverage = Math.max(0, entry.getInteger(COVERAGE_TAG));
            value.wireless = entry.getBoolean(WIRELESS_TAG);
            value.anchor = Anchor.read(entry);
            return value;
        }
    }

    private static final class DistributorConfig {
        private long throughput = UNLIMITED;
        private int machineLimit;
        private int bindingLimit;
        private int coverage = DEFAULT_COVERAGE;
        @Nullable
        private Anchor anchor;

        private static DistributorConfig read(final NBTTagCompound entry) {
            DistributorConfig value = new DistributorConfig();
            value.throughput = Math.max(0L, entry.getLong(THROUGHPUT_TAG));
            value.machineLimit = Math.max(0, entry.getInteger(MACHINE_LIMIT_TAG));
            value.bindingLimit = Math.max(0, entry.getInteger(BINDING_LIMIT_TAG));
            value.coverage = Math.max(0, entry.getInteger(COVERAGE_TAG));
            value.anchor = Anchor.read(entry);
            return value;
        }
    }

    private static final class RouteConfig {
        private String interfaceId = "";
        private String distributorId = "";
        private String connectionType = CONNECTION_WIRED;
        private boolean enabled = true;
        private int dimension;
        private BlockPos position = BlockPos.ORIGIN;

        private static RouteConfig read(final NBTTagCompound entry) {
            RouteConfig value = new RouteConfig();
            value.interfaceId = normalize(entry.getString(INTERFACE_ID_TAG));
            value.distributorId = normalize(entry.getString(DISTRIBUTOR_ID_TAG));
            String type = normalizeConnectionType(entry.getString(CONNECTION_TYPE_TAG));
            value.connectionType = type == null ? "" : type;
            value.enabled = !entry.hasKey(ENABLED_TAG) || entry.getBoolean(ENABLED_TAG);
            value.dimension = entry.getInteger(DIMENSION_TAG);
            value.position = BlockPos.fromLong(entry.getLong(POSITION_TAG));
            return value;
        }
    }

    private static final class Anchor {
        private final int dimension;
        private final BlockPos position;

        private Anchor(final int dimension, final BlockPos position) {
            this.dimension = dimension;
            this.position = position;
        }

        @Nullable
        private static Anchor read(final NBTTagCompound entry) {
            if (!entry.hasKey(DIMENSION_TAG, Constants.NBT.TAG_INT)
                || !entry.hasKey(POSITION_TAG, Constants.NBT.TAG_LONG)) {
                return null;
            }
            return new Anchor(
                entry.getInteger(DIMENSION_TAG),
                BlockPos.fromLong(entry.getLong(POSITION_TAG))
            );
        }
    }

    private static final class RuntimeNetwork {
        private final Map<Long, Map<String, NodeReport>> reportsByTick = new HashMap<>();
        private final Map<String, NodeResult> results = new HashMap<>();
        private long lastSettledTick = -1L;
        private long totalCpuOutput;
        private long totalDemand;
        private long totalAllocated;
        private int eligibleNodes;
        private int rejectedNodes;
    }

    private static final class NodeReport {
        private String interfaceId = "";
        private String distributorId = "";
        private BlockPos nodePos = BlockPos.ORIGIN;
        private long cpuOutput;
        private long demand;
        private long tick = -1L;
        private RouteStatus routeStatus = RouteStatus.UNBOUND;
        private final Map<String, Contribution> contributions = new HashMap<>();
    }

    private static final class Contribution {
        private long cpuOutput;
        private long demand;
    }

    private static final class ContributionEntry {
        private final String id;
        private final Contribution contribution;

        private ContributionEntry(final String id, final Contribution contribution) {
            this.id = id;
            this.contribution = contribution;
        }
    }

    private static final class NodeEntry {
        private final String nodeId;
        private final NodeReport report;

        private NodeEntry(final String nodeId, final NodeReport report) {
            this.nodeId = nodeId;
            this.report = report;
        }
    }

    private static final class NodeResult {
        private final long demand;
        private final long allocated;
        private final RouteStatus routeStatus;
        private final Map<String, ContributionResult> contributions;

        private NodeResult(
            final long demand,
            final long allocated,
            final RouteStatus routeStatus,
            final Map<String, ContributionResult> contributions
        ) {
            this.demand = demand;
            this.allocated = allocated;
            this.routeStatus = routeStatus;
            this.contributions = contributions;
        }

        private long getContributionAllocated(final String contributionId) {
            ContributionResult result = contributions.get(contributionId);
            return result == null ? 0L : result.allocated;
        }

        private boolean isContributionSatisfied(final String contributionId) {
            ContributionResult result = contributions.get(contributionId);
            return result != null && result.demand > 0L && result.allocated >= result.demand;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NodeResult)) {
                return false;
            }
            NodeResult other = (NodeResult) obj;
            return demand == other.demand
                && allocated == other.allocated
                && routeStatus == other.routeStatus
                && contributions.equals(other.contributions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(demand, allocated, routeStatus, contributions);
        }
    }

    private static final class ContributionResult {
        private final long demand;
        private final long allocated;

        private ContributionResult(final long demand, final long allocated) {
            this.demand = demand;
            this.allocated = allocated;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ContributionResult)) {
                return false;
            }
            ContributionResult other = (ContributionResult) obj;
            return demand == other.demand && allocated == other.allocated;
        }

        @Override
        public int hashCode() {
            return Objects.hash(demand, allocated);
        }
    }

    private enum RouteStatus {
        VALID("valid"),
        UNBOUND("unbound"),
        DISABLED("disabled"),
        INVALID_CONTEXT("invalid_context"),
        NODE_MOVED("node_moved"),
        INTERFACE_MISSING("interface_missing"),
        DISTRIBUTOR_MISSING("distributor_missing"),
        CONNECTION_TYPE_MISMATCH("connection_type_mismatch"),
        INTERFACE_ANCHOR_MISSING("interface_anchor_missing"),
        DISTRIBUTOR_ANCHOR_MISSING("distributor_anchor_missing"),
        MATRIX_ANCHOR_MISSING("matrix_anchor_missing"),
        MACHINE_ENDPOINT_MISSING("machine_endpoint_missing"),
        INTERFACE_ENDPOINT_MISSING("interface_endpoint_missing"),
        DISTRIBUTOR_ENDPOINT_MISSING("distributor_endpoint_missing"),
        MATRIX_ENDPOINT_MISSING("matrix_endpoint_missing"),
        WIRED_PATH_INVALID("wired_path_invalid"),
        WIRELESS_BACKBONE_INVALID("wireless_backbone_invalid"),
        DIMENSION_MISMATCH("dimension_mismatch"),
        OUT_OF_RANGE("out_of_range"),
        DISTRIBUTOR_OUT_OF_RANGE("distributor_out_of_range");

        private final String id;

        RouteStatus(final String id) {
            this.id = id;
        }
    }

    private static final class RouteResolution {
        @Nullable
        private final RouteConfig route;
        private final RouteStatus status;

        private RouteResolution(
            @Nullable final RouteConfig route,
            final RouteStatus status
        ) {
            this.route = route;
            this.status = status;
        }

        private static RouteResolution of(
            final RouteConfig route,
            final RouteStatus status
        ) {
            return new RouteResolution(route, status);
        }

        private static RouteResolution invalid(final RouteStatus status) {
            return new RouteResolution(null, status);
        }

        private boolean isValid() {
            return route != null && status == RouteStatus.VALID;
        }
    }

    private static final class NetworkKey {
        private final int dimension;
        private final String networkId;

        private NetworkKey(final int dimension, final String networkId) {
            this.dimension = dimension;
            this.networkId = networkId;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NetworkKey)) {
                return false;
            }
            NetworkKey other = (NetworkKey) obj;
            return dimension == other.dimension && networkId.equals(other.networkId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, networkId);
        }
    }

    private interface TopologyMutation {
        boolean apply(NBTTagCompound data);
    }
}

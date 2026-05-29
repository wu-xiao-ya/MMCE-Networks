package com.mmce.networks.common.handler;

import com.mmce.networks.common.data.NetworkResourcePool;
import com.mmce.networks.common.util.WorldCompat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public final class TransientSupplyScheduler {
    private static final Object LOCK = new Object();
    private static final PriorityQueue<ExpiryEntry> EXPIRIES = new PriorityQueue<>();
    private static final Map<SourceKey, ActiveSupply> ACTIVE_SUPPLIES = new HashMap<>();
    private static final Map<PoolKey, Long> TOTALS = new HashMap<>();
    private static final Map<NetworkKey, Map<SourceKey, ActiveSupply>> ACTIVE_BY_NETWORK = new HashMap<>();

    private TransientSupplyScheduler() {
    }

    public static long pulse(
        final World world,
        final int dimension,
        final String networkId,
        final String key,
        final String source,
        final long amount,
        final long expiresAt
    ) {
        if (world == null || WorldCompat.isRemote(world) || isNullOrEmpty(networkId) || isNullOrEmpty(key) || isNullOrEmpty(source)) {
            return 0L;
        }

        SourceKey sourceKey = new SourceKey(dimension, networkId, key, source);
        PoolKey poolKey = sourceKey.asPoolKey();
        NetworkKey networkKey = sourceKey.asNetworkKey();
        boolean changed = false;
        synchronized (LOCK) {
            ActiveSupply current = ACTIVE_SUPPLIES.get(sourceKey);
            if (current == null || current.amount != amount || current.expiresAt != expiresAt) {
                changed = true;
            }
            ActiveSupply next = new ActiveSupply(amount, expiresAt);
            ACTIVE_SUPPLIES.put(sourceKey, next);
            ACTIVE_BY_NETWORK.computeIfAbsent(networkKey, ignored -> new HashMap<>()).put(sourceKey, next);
            long currentAmount = current == null ? 0L : current.amount;
            long delta = amount - currentAmount;
            if (delta != 0L) {
                updateTotal(poolKey, delta);
            }
            EXPIRIES.offer(new ExpiryEntry(sourceKey, expiresAt));
        }
        if (changed) {
            ControllerNetworkSyncHandler.markNetworkDirty(world, networkId);
        }
        return getTransientSupplyTotal(poolKey);
    }

    public static void process(final World world, final int dimension) {
        if (world == null || WorldCompat.isRemote(world)) {
            return;
        }

        long now = System.currentTimeMillis();
        while (true) {
            ExpiryEntry entry;
            synchronized (LOCK) {
                entry = EXPIRIES.peek();
                if (entry == null || entry.expiresAt > now) {
                    return;
                }
                EXPIRIES.poll();

                ActiveSupply current = ACTIVE_SUPPLIES.get(entry.sourceKey);
                if (current == null || current.expiresAt != entry.expiresAt) {
                    continue;
                }
                ACTIVE_SUPPLIES.remove(entry.sourceKey);
                updateTotal(entry.sourceKey.asPoolKey(), -current.amount);
                removeFromNetworkIndex(entry.sourceKey, current);
            }

            if (entry.sourceKey.dimension == dimension) {
                ControllerNetworkSyncHandler.markNetworkDirty(world, entry.sourceKey.networkId);
            }
        }
    }

    public static void applyTransientSupplies(final NBTTagCompound sharedData, final int dimension, final String networkId) {
        if (sharedData == null || isNullOrEmpty(networkId)) {
            return;
        }

        synchronized (LOCK) {
            Map<SourceKey, ActiveSupply> networkSupplies = ACTIVE_BY_NETWORK.get(new NetworkKey(dimension, networkId));
            if (networkSupplies == null || networkSupplies.isEmpty()) {
                return;
            }
            for (Map.Entry<SourceKey, ActiveSupply> entry : networkSupplies.entrySet()) {
                SourceKey key = entry.getKey();
                ActiveSupply supply = entry.getValue();
                NetworkResourcePool.setTransientSupply(sharedData, key.poolKey, key.source, supply.amount, supply.expiresAt);
            }
        }
    }

    public static long getTransientSupplyTotal(final int dimension, final String networkId, final String key) {
        synchronized (LOCK) {
            return getTransientSupplyTotal(new PoolKey(dimension, networkId, key));
        }
    }

    private static long getTransientSupplyTotal(final PoolKey poolKey) {
        Long total = TOTALS.get(poolKey);
        return total == null ? 0L : total.longValue();
    }

    private static void updateTotal(final PoolKey poolKey, final long delta) {
        if (delta == 0L) {
            return;
        }
        long next = getTransientSupplyTotal(poolKey) + delta;
        if (next <= 0L) {
            TOTALS.remove(poolKey);
        } else {
            TOTALS.put(poolKey, next);
        }
    }

    private static void removeFromNetworkIndex(final SourceKey sourceKey, final ActiveSupply current) {
        Map<SourceKey, ActiveSupply> networkSupplies = ACTIVE_BY_NETWORK.get(sourceKey.asNetworkKey());
        if (networkSupplies == null) {
            return;
        }
        ActiveSupply indexed = networkSupplies.get(sourceKey);
        if (indexed != current) {
            return;
        }
        networkSupplies.remove(sourceKey);
        if (networkSupplies.isEmpty()) {
            ACTIVE_BY_NETWORK.remove(sourceKey.asNetworkKey());
        }
    }

    private static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    private static final class ActiveSupply {
        private final long amount;
        private final long expiresAt;

        private ActiveSupply(final long amount, final long expiresAt) {
            this.amount = amount;
            this.expiresAt = expiresAt;
        }
    }

    private static final class ExpiryEntry implements Comparable<ExpiryEntry> {
        private final SourceKey sourceKey;
        private final long expiresAt;

        private ExpiryEntry(final SourceKey sourceKey, final long expiresAt) {
            this.sourceKey = sourceKey;
            this.expiresAt = expiresAt;
        }

        @Override
        public int compareTo(final ExpiryEntry other) {
            return Long.compare(expiresAt, other.expiresAt);
        }
    }

    private static final class SourceKey {
        private final int dimension;
        private final String networkId;
        private final String poolKey;
        private final String source;

        private SourceKey(final int dimension, final String networkId, final String poolKey, final String source) {
            this.dimension = dimension;
            this.networkId = networkId;
            this.poolKey = poolKey;
            this.source = source;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SourceKey)) {
                return false;
            }
            SourceKey other = (SourceKey) obj;
            return dimension == other.dimension
                && networkId.equals(other.networkId)
                && poolKey.equals(other.poolKey)
                && source.equals(other.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, networkId, poolKey, source);
        }

        private PoolKey asPoolKey() {
            return new PoolKey(dimension, networkId, poolKey);
        }

        private NetworkKey asNetworkKey() {
            return new NetworkKey(dimension, networkId);
        }
    }

    private static final class PoolKey {
        private final int dimension;
        private final String networkId;
        private final String poolKey;

        private PoolKey(final int dimension, final String networkId, final String poolKey) {
            this.dimension = dimension;
            this.networkId = networkId;
            this.poolKey = poolKey;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PoolKey)) {
                return false;
            }
            PoolKey other = (PoolKey) obj;
            return dimension == other.dimension
                && networkId.equals(other.networkId)
                && poolKey.equals(other.poolKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, networkId, poolKey);
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
}

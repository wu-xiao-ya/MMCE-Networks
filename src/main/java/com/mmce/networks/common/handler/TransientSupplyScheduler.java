package com.mmce.networks.common.handler;

import com.mmce.networks.common.data.NetworkResourcePool;
import com.mmce.networks.common.util.WorldCompat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

public final class TransientSupplyScheduler {
    private static final Object LOCK = new Object();
    private static final PriorityQueue<ExpiryEntry> EXPIRIES = new PriorityQueue<>();
    private static final Map<SourceKey, ActiveSupply> ACTIVE_SUPPLIES = new HashMap<>();

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
        boolean changed = false;
        synchronized (LOCK) {
            ActiveSupply current = ACTIVE_SUPPLIES.get(sourceKey);
            if (current == null || current.amount != amount) {
                changed = true;
            }
            ACTIVE_SUPPLIES.put(sourceKey, new ActiveSupply(amount, expiresAt));
            EXPIRIES.offer(new ExpiryEntry(sourceKey, expiresAt));
        }
        if (changed) {
            ControllerNetworkSyncHandler.markNetworkDirty(world, networkId);
        }
        return getTransientSupplyTotal(dimension, networkId, key);
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
            for (Map.Entry<SourceKey, ActiveSupply> entry : ACTIVE_SUPPLIES.entrySet()) {
                SourceKey key = entry.getKey();
                if (key.dimension != dimension || !key.networkId.equals(networkId)) {
                    continue;
                }
                ActiveSupply supply = entry.getValue();
                NetworkResourcePool.setTransientSupply(sharedData, key.poolKey, key.source, supply.amount, supply.expiresAt);
            }
        }
    }

    public static long getTransientSupplyTotal(final int dimension, final String networkId, final String key) {
        long total = 0L;
        synchronized (LOCK) {
            for (Map.Entry<SourceKey, ActiveSupply> entry : ACTIVE_SUPPLIES.entrySet()) {
                SourceKey sourceKey = entry.getKey();
                if (sourceKey.dimension == dimension && sourceKey.networkId.equals(networkId) && sourceKey.poolKey.equals(key)) {
                    total += entry.getValue().amount;
                }
            }
        }
        return total;
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
    }
}

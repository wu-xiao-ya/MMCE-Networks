package com.mmce.networks.common.data;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTPrimitive;

import javax.annotation.Nullable;

public final class NetworkResourcePool {
    private static final String ROOT_TAG = "_resourcePools";
    private static final String PROVIDERS_TAG = "providers";
    private static final String CONSUMERS_TAG = "consumers";

    private NetworkResourcePool() {
    }

    public static long getTotalSupply(final NBTTagCompound sharedData, final String key) {
        return sumEntries(getEntries(sharedData, key, PROVIDERS_TAG, false));
    }

    public static long getTotalUsage(final NBTTagCompound sharedData, final String key) {
        return sumEntries(getEntries(sharedData, key, CONSUMERS_TAG, false));
    }

    public static long getAvailable(final NBTTagCompound sharedData, final String key) {
        return Math.max(0L, getTotalSupply(sharedData, key) - getTotalUsage(sharedData, key));
    }

    public static long getSupply(final NBTTagCompound sharedData, final String key, final String source) {
        return getEntryAmount(getEntries(sharedData, key, PROVIDERS_TAG, false), source);
    }

    public static long getUsage(final NBTTagCompound sharedData, final String key, final String source) {
        return getEntryAmount(getEntries(sharedData, key, CONSUMERS_TAG, false), source);
    }

    public static long setSupply(final NBTTagCompound sharedData, final String key, final String source, final long amount) {
        setEntryAmount(getEntries(sharedData, key, PROVIDERS_TAG, true), source, amount);
        cleanupPool(sharedData, key);
        return getTotalSupply(sharedData, key);
    }

    public static boolean trySetUsage(final NBTTagCompound sharedData, final String key, final String source, final long amount) {
        if (amount < 0L) {
            return false;
        }

        long capacity = getTotalSupply(sharedData, key);
        NBTTagCompound consumers = getEntries(sharedData, key, CONSUMERS_TAG, true);
        long current = getEntryAmount(consumers, source);
        long usedWithoutSource = getTotalUsage(sharedData, key) - current;
        if (usedWithoutSource + amount > capacity) {
            return false;
        }

        setEntryAmount(consumers, source, amount);
        cleanupPool(sharedData, key);
        return true;
    }

    public static boolean tryAddUsage(final NBTTagCompound sharedData, final String key, final String source, final long amount) {
        if (amount < 0L) {
            return false;
        }
        return trySetUsage(sharedData, key, source, getUsage(sharedData, key, source) + amount);
    }

    public static long releaseUsage(final NBTTagCompound sharedData, final String key, final String source, final long amount) {
        if (amount <= 0L) {
            return getUsage(sharedData, key, source);
        }

        long current = getUsage(sharedData, key, source);
        long next = Math.max(0L, current - amount);
        NBTTagCompound consumers = getEntries(sharedData, key, CONSUMERS_TAG, true);
        setEntryAmount(consumers, source, next);
        cleanupPool(sharedData, key);
        return next;
    }

    public static long clearSupply(final NBTTagCompound sharedData, final String key, final String source) {
        return setSupply(sharedData, key, source, 0L);
    }

    public static boolean clearUsage(final NBTTagCompound sharedData, final String key, final String source) {
        return trySetUsage(sharedData, key, source, 0L);
    }

    @Nullable
    public static NBTTagCompound getPoolSnapshot(final NBTTagCompound sharedData, final String key) {
        NBTTagCompound pool = getPool(sharedData, key, false);
        if (pool == null) {
            return null;
        }

        NBTTagCompound snapshot = pool.copy();
        snapshot.setLong("capacity", getTotalSupply(sharedData, key));
        snapshot.setLong("used", getTotalUsage(sharedData, key));
        snapshot.setLong("available", getAvailable(sharedData, key));
        return snapshot;
    }

    public static NBTTagCompound getAllPoolsSnapshot(final NBTTagCompound sharedData) {
        NBTTagCompound snapshot = new NBTTagCompound();
        NBTTagCompound root = getRoot(sharedData, false);
        if (root == null) {
            return snapshot;
        }

        for (String key : root.getKeySet()) {
            NBTTagCompound poolSnapshot = getPoolSnapshot(sharedData, key);
            if (poolSnapshot != null) {
                snapshot.setTag(key, poolSnapshot);
            }
        }
        return snapshot;
    }

    @Nullable
    private static NBTTagCompound getPool(final NBTTagCompound sharedData, final String key, final boolean create) {
        if (sharedData == null || isNullOrEmpty(key)) {
            return null;
        }

        NBTTagCompound root = getRoot(sharedData, create);
        if (root == null) {
            return null;
        }
        if (root.hasKey(key, 10)) {
            return root.getCompoundTag(key);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound pool = new NBTTagCompound();
        root.setTag(key, pool);
        return pool;
    }

    @Nullable
    private static NBTTagCompound getEntries(final NBTTagCompound sharedData, final String key, final String entriesKey, final boolean create) {
        NBTTagCompound pool = getPool(sharedData, key, create);
        if (pool == null) {
            return null;
        }
        if (pool.hasKey(entriesKey, 10)) {
            return pool.getCompoundTag(entriesKey);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound entries = new NBTTagCompound();
        pool.setTag(entriesKey, entries);
        return entries;
    }

    @Nullable
    private static NBTTagCompound getRoot(final NBTTagCompound sharedData, final boolean create) {
        if (sharedData == null) {
            return null;
        }
        if (sharedData.hasKey(ROOT_TAG, 10)) {
            return sharedData.getCompoundTag(ROOT_TAG);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound root = new NBTTagCompound();
        sharedData.setTag(ROOT_TAG, root);
        return root;
    }

    private static long sumEntries(@Nullable final NBTTagCompound entries) {
        if (entries == null) {
            return 0L;
        }

        long total = 0L;
        for (String source : entries.getKeySet()) {
            total += getEntryAmount(entries, source);
        }
        return total;
    }

    private static long getEntryAmount(@Nullable final NBTTagCompound entries, final String source) {
        if (entries == null || isNullOrEmpty(source) || !entries.hasKey(source)) {
            return 0L;
        }

        NBTBase tag = entries.getTag(source);
        return tag instanceof NBTPrimitive ? ((NBTPrimitive) tag).getLong() : 0L;
    }

    private static void setEntryAmount(@Nullable final NBTTagCompound entries, final String source, final long amount) {
        if (entries == null || isNullOrEmpty(source)) {
            return;
        }
        if (amount <= 0L) {
            entries.removeTag(source);
        } else {
            entries.setLong(source, amount);
        }
    }

    private static void cleanupPool(final NBTTagCompound sharedData, final String key) {
        NBTTagCompound root = getRoot(sharedData, false);
        NBTTagCompound pool = getPool(sharedData, key, false);
        if (root == null || pool == null) {
            return;
        }

        cleanupEntries(pool, PROVIDERS_TAG);
        cleanupEntries(pool, CONSUMERS_TAG);
        if (pool.getKeySet().isEmpty()) {
            root.removeTag(key);
        }
        if (root.getKeySet().isEmpty()) {
            sharedData.removeTag(ROOT_TAG);
        }
    }

    private static void cleanupEntries(final NBTTagCompound pool, final String entriesKey) {
        if (!pool.hasKey(entriesKey, 10)) {
            return;
        }

        NBTTagCompound entries = pool.getCompoundTag(entriesKey);
        if (entries.getKeySet().isEmpty()) {
            pool.removeTag(entriesKey);
        }
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }
}

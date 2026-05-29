package com.mmce.networks.api;

import com.mmce.networks.common.data.NetworkResourcePool;
import com.mmce.networks.common.data.NetworkTechTree;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.handler.ControllerNetworkSyncHandler;
import com.mmce.networks.common.handler.TransientSupplyScheduler;
import com.mmce.networks.common.util.WorldCompat;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class MMCENetworkApi {
    private MMCENetworkApi() {
    }

    public static NBTTagCompound getSharedData(final World world, final String networkId) {
        int dimension = WorldCompat.getDimension(world);
        NBTTagCompound sharedData = MMCENetworkSavedData.get(world).getNetworkData(dimension, networkId);
        TransientSupplyScheduler.applyTransientSupplies(sharedData, dimension, networkId);
        return sharedData;
    }

    public static void setSharedData(final World world, final String networkId, final NBTTagCompound sharedData) {
        MMCENetworkSavedData.get(world).putNetworkData(WorldCompat.getDimension(world), networkId, sharedData);
        ControllerNetworkSyncHandler.markNetworkDirty(world, networkId);
    }

    public static NBTBase getValue(final World world, final String networkId, final String key) {
        NBTTagCompound data = getSharedData(world, networkId);
        return data.hasKey(key) ? data.getTag(key).copy() : null;
    }

    public static void setValue(final World world, final String networkId, final String key, final NBTBase value) {
        NBTTagCompound data = getSharedData(world, networkId);
        if (value == null) {
            data.removeTag(key);
        } else {
            data.setTag(key, value.copy());
        }
        setSharedData(world, networkId, data);
    }

    public static long getResourceCapacity(final World world, final String networkId, final String key) {
        return withSharedData(world, networkId, data -> NetworkResourcePool.getTotalSupply(data, key));
    }

    public static long getResourceUsage(final World world, final String networkId, final String key) {
        return withSharedData(world, networkId, data -> NetworkResourcePool.getTotalUsage(data, key));
    }

    public static long getResourceAvailable(final World world, final String networkId, final String key) {
        return withSharedData(world, networkId, data -> NetworkResourcePool.getAvailable(data, key));
    }

    public static long getResourceSupply(final World world, final String networkId, final String key, final String source) {
        return withSharedData(world, networkId, data -> NetworkResourcePool.getSupply(data, key, source));
    }

    public static long getResourceUsage(final World world, final String networkId, final String key, final String source) {
        return withSharedData(world, networkId, data -> NetworkResourcePool.getUsage(data, key, source));
    }

    public static long setResourceSupply(final World world, final String networkId, final String key, final String source, final long amount) {
        return mutateSharedData(world, networkId, data -> NetworkResourcePool.setSupply(data, key, source, amount));
    }

    public static long clearResourceSupply(final World world, final String networkId, final String key, final String source) {
        return mutateSharedData(world, networkId, data -> NetworkResourcePool.clearSupply(data, key, source));
    }

    public static boolean trySetResourceUsage(final World world, final String networkId, final String key, final String source, final long amount) {
        return mutateSharedData(world, networkId, data -> NetworkResourcePool.trySetUsage(data, key, source, amount));
    }

    public static boolean tryAddResourceUsage(final World world, final String networkId, final String key, final String source, final long amount) {
        return mutateSharedData(world, networkId, data -> NetworkResourcePool.tryAddUsage(data, key, source, amount));
    }

    public static long releaseResourceUsage(final World world, final String networkId, final String key, final String source, final long amount) {
        return mutateSharedData(world, networkId, data -> NetworkResourcePool.releaseUsage(data, key, source, amount));
    }

    public static boolean clearResourceUsage(final World world, final String networkId, final String key, final String source) {
        return mutateSharedData(world, networkId, data -> NetworkResourcePool.clearUsage(data, key, source));
    }

    public static NBTTagCompound getAllResourcePools(final World world, final String networkId) {
        return withSharedData(world, networkId, NetworkResourcePool::getAllPoolsSnapshot);
    }

    public static NBTTagCompound getResourcePool(final World world, final String networkId, final String key) {
        return withSharedData(world, networkId, data -> {
            NBTTagCompound snapshot = NetworkResourcePool.getPoolSnapshot(data, key);
            return snapshot == null ? new NBTTagCompound() : snapshot;
        });
    }

    public static boolean defineTech(final World world, final String networkId, final String techId) {
        return mutateSharedData(world, networkId, data -> NetworkTechTree.defineTech(data, techId));
    }

    public static boolean removeTech(final World world, final String networkId, final String techId) {
        return mutateSharedData(world, networkId, data -> NetworkTechTree.removeTech(data, techId));
    }

    public static boolean addTechPrerequisite(final World world, final String networkId, final String techId, final String prerequisiteId) {
        return mutateSharedData(world, networkId, data -> NetworkTechTree.addPrerequisite(data, techId, prerequisiteId));
    }

    public static boolean removeTechPrerequisite(final World world, final String networkId, final String techId, final String prerequisiteId) {
        return mutateSharedData(world, networkId, data -> NetworkTechTree.removePrerequisite(data, techId, prerequisiteId));
    }

    public static boolean hasTechDefinition(final World world, final String networkId, final String techId) {
        return withSharedData(world, networkId, data -> NetworkTechTree.hasDefinition(data, techId));
    }

    public static boolean hasTechUnlocked(final World world, final String networkId, final String techId) {
        return withSharedData(world, networkId, data -> NetworkTechTree.isUnlocked(data, techId));
    }

    public static boolean canUnlockTech(final World world, final String networkId, final String techId) {
        return withSharedData(world, networkId, data -> NetworkTechTree.canUnlock(data, techId));
    }

    public static boolean unlockTech(final World world, final String networkId, final String techId) {
        return mutateSharedData(world, networkId, data -> NetworkTechTree.unlock(data, techId));
    }

    public static boolean lockTech(final World world, final String networkId, final String techId) {
        return mutateSharedData(world, networkId, data -> NetworkTechTree.lock(data, techId));
    }

    public static NBTTagCompound getTechTree(final World world, final String networkId) {
        return withSharedData(world, networkId, NetworkTechTree::getTreeSnapshot);
    }

    public static NBTTagCompound getTech(final World world, final String networkId, final String techId) {
        return withSharedData(world, networkId, data -> {
            NBTTagCompound snapshot = NetworkTechTree.getTechSnapshot(data, techId);
            return snapshot == null ? new NBTTagCompound() : snapshot;
        });
    }

    private static <T> T withSharedData(final World world, final String networkId, final SharedDataOperation<T> operation) {
        MMCENetworkSavedData savedData = MMCENetworkSavedData.get(world);
        synchronized (savedData) {
            return operation.run(savedData.getNetworkData(WorldCompat.getDimension(world), networkId));
        }
    }

    private static <T> T mutateSharedData(final World world, final String networkId, final SharedDataOperation<T> operation) {
        MMCENetworkSavedData savedData = MMCENetworkSavedData.get(world);
        int dimension = WorldCompat.getDimension(world);
        synchronized (savedData) {
            NBTTagCompound data = savedData.getNetworkDataMutable(dimension, networkId);
            T result = operation.run(data);
            savedData.markDirty();
            ControllerNetworkSyncHandler.markNetworkDirty(world, networkId);
            return result;
        }
    }

    private interface SharedDataOperation<T> {
        T run(NBTTagCompound data);
    }
}

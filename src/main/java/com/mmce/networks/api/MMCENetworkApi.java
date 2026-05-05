package com.mmce.networks.api;

import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.handler.ControllerNetworkSyncHandler;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class MMCENetworkApi {
    private MMCENetworkApi() {
    }

    public static NBTTagCompound getSharedData(final World world, final String networkId) {
        return MMCENetworkSavedData.get(world).getNetworkData(world.provider.getDimension(), networkId);
    }

    public static void setSharedData(final World world, final String networkId, final NBTTagCompound sharedData) {
        MMCENetworkSavedData.get(world).putNetworkData(world.provider.getDimension(), networkId, sharedData);
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
}

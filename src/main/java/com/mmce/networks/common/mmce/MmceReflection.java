package com.mmce.networks.common.mmce;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MmceReflection {
    private static final String CONTROLLER_TILE_CLASS = "hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController";

    private static final String ROOT_TAG = "mmceNetworks";
    private static final String NETWORK_ID_TAG = "networkId";
    private static final String SHARED_DATA_TAG = "sharedData";

    private final Class<?> controllerTileClass;
    private final Method getCustomDataTagMethod;
    private final Method setCustomDataTagMethod;
    private final Method markForUpdateSyncMethod;

    public MmceReflection() {
        Class<?> tileClass = null;
        Method getMethod = null;
        Method setMethod = null;
        Method syncMethod = null;

        try {
            tileClass = Class.forName(CONTROLLER_TILE_CLASS);
            getMethod = tileClass.getMethod("getCustomDataTag");
            setMethod = tileClass.getMethod("setCustomDataTag", NBTTagCompound.class);
            syncMethod = tileClass.getMethod("markForUpdateSync");
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }

        this.controllerTileClass = tileClass;
        this.getCustomDataTagMethod = getMethod;
        this.setCustomDataTagMethod = setMethod;
        this.markForUpdateSyncMethod = syncMethod;
    }

    public boolean isAvailable() {
        return controllerTileClass != null
            && getCustomDataTagMethod != null
            && setCustomDataTagMethod != null
            && markForUpdateSyncMethod != null;
    }

    public boolean isControllerTile(final TileEntity tile) {
        return tile != null && controllerTileClass != null && controllerTileClass.isInstance(tile);
    }

    public String getBoundNetworkId(final TileEntity tile) {
        NBTTagCompound rootTag = getRootTag(tile);
        if (rootTag == null || !rootTag.hasKey(NETWORK_ID_TAG, Constants.NBT.TAG_STRING)) {
            return null;
        }
        return rootTag.getString(NETWORK_ID_TAG);
    }

    public NBTTagCompound getSharedData(final TileEntity tile) {
        NBTTagCompound rootTag = getRootTag(tile);
        if (rootTag == null || !rootTag.hasKey(SHARED_DATA_TAG, Constants.NBT.TAG_COMPOUND)) {
            return new NBTTagCompound();
        }
        return rootTag.getCompoundTag(SHARED_DATA_TAG).copy();
    }

    public boolean setSharedData(final TileEntity tile, final String networkId, final NBTTagCompound sharedData) {
        if (!isControllerTile(tile)) {
            return false;
        }

        NBTTagCompound customData = getMutableCustomData(tile);
        NBTTagCompound rootTag = customData.hasKey(ROOT_TAG, Constants.NBT.TAG_COMPOUND)
            ? customData.getCompoundTag(ROOT_TAG).copy()
            : new NBTTagCompound();
        rootTag.setString(NETWORK_ID_TAG, networkId);
        rootTag.setTag(SHARED_DATA_TAG, sharedData.copy());
        customData.setTag(ROOT_TAG, rootTag);
        setCustomDataTag(tile, customData);
        return true;
    }

    public boolean clearSharedData(final TileEntity tile) {
        if (!isControllerTile(tile)) {
            return false;
        }

        NBTTagCompound customData = getMutableCustomData(tile);
        customData.removeTag(ROOT_TAG);
        setCustomDataTag(tile, customData);
        return true;
    }

    public void markForUpdateSync(final TileEntity tile) {
        invoke(markForUpdateSyncMethod, tile);
    }

    private NBTTagCompound getRootTag(final TileEntity tile) {
        NBTTagCompound customData = getCustomDataTag(tile);
        if (customData == null || !customData.hasKey(ROOT_TAG, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        return customData.getCompoundTag(ROOT_TAG).copy();
    }

    private NBTTagCompound getMutableCustomData(final TileEntity tile) {
        NBTTagCompound customData = getCustomDataTag(tile);
        return customData == null ? new NBTTagCompound() : customData.copy();
    }

    private NBTTagCompound getCustomDataTag(final TileEntity tile) {
        Object result = invoke(getCustomDataTagMethod, tile);
        return result instanceof NBTTagCompound ? (NBTTagCompound) result : null;
    }

    private void setCustomDataTag(final TileEntity tile, final NBTTagCompound tag) {
        invoke(setCustomDataTagMethod, tile, tag);
    }

    private Object invoke(final Method method, final Object instance, final Object... args) {
        if (method == null || instance == null) {
            return null;
        }

        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }
}

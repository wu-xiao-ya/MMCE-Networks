package com.mmce.networks.client.gui;

import com.mmce.networks.common.data.NetworkValueDisplayRegistry.ValueDisplaySpec;
import com.mmce.networks.common.network.MessageRequestNetworkSnapshot;
import com.mmce.networks.common.network.NetworkHandler;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class NetworkTerminalClientState {
    private static String activeNetworkId = "";
    private static NBTTagCompound sharedData = new NBTTagCompound();
    private static List<ValueDisplaySpec> valueDisplaySpecs = new ArrayList<>();
    private static long lastUpdatedAt = -1L;
    private static boolean loading;

    private NetworkTerminalClientState() {
    }

    public static void open(final String networkId) {
        activeNetworkId = networkId == null ? "" : networkId;
        loading = true;
        requestRefresh();
    }

    public static void requestRefresh() {
        if (activeNetworkId.isEmpty()) {
            return;
        }
        loading = true;
        NetworkHandler.CHANNEL.sendToServer(new MessageRequestNetworkSnapshot(activeNetworkId));
    }

    public static void applySnapshot(final String networkId, @Nullable final NBTTagCompound data, @Nullable final List<ValueDisplaySpec> specs) {
        if (networkId == null || !networkId.equals(activeNetworkId)) {
            return;
        }
        sharedData = data == null ? new NBTTagCompound() : data.copy();
        valueDisplaySpecs = specs == null ? new ArrayList<>() : new ArrayList<>(specs);
        lastUpdatedAt = System.currentTimeMillis();
        loading = false;
    }

    public static String getActiveNetworkId() {
        return activeNetworkId;
    }

    public static NBTTagCompound getSharedData() {
        return sharedData.copy();
    }

    public static List<ValueDisplaySpec> getValueDisplaySpecs() {
        return new ArrayList<>(valueDisplaySpecs);
    }

    public static long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public static boolean isLoading() {
        return loading;
    }
}

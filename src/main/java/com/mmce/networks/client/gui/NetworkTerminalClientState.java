package com.mmce.networks.client.gui;

import com.mmce.networks.common.data.NetworkValueDisplayRegistry.ValueDisplaySpec;
import com.mmce.networks.common.network.MessageRequestNetworkSnapshot;
import com.mmce.networks.common.network.NetworkHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NetworkTerminalClientState {
    private static String activeNetworkId = "";
    private static NBTTagCompound sharedData = new NBTTagCompound();
    private static List<ValueDisplaySpec> valueDisplaySpecs = new ArrayList<>();
    private static String valueDisplayLayout = "list";
    private static List<NetworkSummary> availableNetworks = new ArrayList<>();
    private static long lastUpdatedAt = -1L;
    private static int stateRevision;
    private static boolean loading;

    private NetworkTerminalClientState() {
    }

    public static void open(final String networkId) {
        activeNetworkId = networkId == null ? "" : networkId;
        loading = true;
        stateRevision++;
        requestRefresh();
    }

    public static void requestRefresh() {
        if (activeNetworkId.isEmpty()) {
            return;
        }
        loading = true;
        NetworkHandler.CHANNEL.sendToServer(new MessageRequestNetworkSnapshot(activeNetworkId));
    }

    public static void applySnapshot(
        final String networkId,
        @Nullable final NBTTagCompound data,
        @Nullable final List<ValueDisplaySpec> specs,
        @Nullable final String layout,
        @Nullable final List<NetworkSummary> networks
    ) {
        if (networkId == null || !networkId.equals(activeNetworkId)) {
            return;
        }
        sharedData = data == null ? new NBTTagCompound() : data.copy();
        valueDisplaySpecs = specs == null ? new ArrayList<>() : new ArrayList<>(specs);
        valueDisplayLayout = layout == null || layout.isEmpty() ? "list" : layout;
        availableNetworks = networks == null ? new ArrayList<>() : new ArrayList<>(networks);
        lastUpdatedAt = System.currentTimeMillis();
        loading = false;
        stateRevision++;
    }

    public static void selectNetwork(final String networkId) {
        if (networkId == null || networkId.isEmpty() || networkId.equals(activeNetworkId)) {
            return;
        }
        activeNetworkId = networkId;
        sharedData = new NBTTagCompound();
        loading = true;
        stateRevision++;
        requestRefresh();
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

    public static String getValueDisplayLayout() {
        return valueDisplayLayout;
    }

    public static List<NetworkSummary> getAvailableNetworks() {
        return new ArrayList<>(availableNetworks);
    }

    public static void applyLocalStyle(final String networkId, final int color, final boolean pinned) {
        if (networkId == null || networkId.isEmpty()) {
            return;
        }
        List<NetworkSummary> updated = new ArrayList<>();
        boolean changed = false;
        for (NetworkSummary summary : availableNetworks) {
            if (summary.networkId.equals(networkId)) {
                updated.add(new NetworkSummary(summary.networkId, summary.displayName, color, pinned));
                changed = true;
            } else {
                updated.add(summary);
            }
        }
        if (!changed) {
            return;
        }
        sortNetworkSummaries(updated);
        availableNetworks = updated;
        stateRevision++;
    }

    public static List<String> getAvailableNetworkIds() {
        List<String> ids = new ArrayList<>();
        for (NetworkSummary summary : availableNetworks) {
            ids.add(summary.networkId);
        }
        return ids;
    }

    public static String getActiveNetworkDisplayName() {
        for (NetworkSummary summary : availableNetworks) {
            if (summary.networkId.equals(activeNetworkId)) {
                return summary.displayName;
            }
        }
        return activeNetworkId;
    }

    public static long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public static boolean isLoading() {
        return loading;
    }

    public static int getStateRevision() {
        return stateRevision;
    }

    public static List<NetworkSummary> readNetworkSummaries(@Nullable final NBTTagCompound root) {
        List<NetworkSummary> result = new ArrayList<>();
        if (root == null || !root.hasKey("entries", Constants.NBT.TAG_LIST)) {
            return result;
        }

        NBTTagList entries = root.getTagList("entries", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            String id = entry.getString("id");
            String displayName = entry.getString("displayName");
            if (id != null && !id.isEmpty()) {
                result.add(new NetworkSummary(
                    id,
                    displayName == null || displayName.isEmpty() ? id : displayName,
                    entry.hasKey("color") ? entry.getInteger("color") : 0xFF6E859D,
                    entry.getBoolean("pinned")
                ));
            }
        }
        sortNetworkSummaries(result);
        return result;
    }

    private static void sortNetworkSummaries(final List<NetworkSummary> summaries) {
        summaries.sort(Comparator
            .comparing(NetworkSummary::isPinned).reversed()
            .thenComparing(NetworkSummary::getDisplayName, String.CASE_INSENSITIVE_ORDER)
        );
    }

    public static final class NetworkSummary {
        private final String networkId;
        private final String displayName;
        private final int color;
        private final boolean pinned;

        public NetworkSummary(final String networkId, final String displayName, final int color, final boolean pinned) {
            this.networkId = networkId;
            this.displayName = displayName;
            this.color = color;
            this.pinned = pinned;
        }

        public String getNetworkId() {
            return networkId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }

        public boolean isPinned() {
            return pinned;
        }
    }
}

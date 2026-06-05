package com.mmce.networks.common.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MMCENetworkSavedData extends WorldSavedData {
    private static final String DATA_NAME = "mmcenetworks_data";
    private static final String DEFAULT_NETWORK_NAME_PREFIX = "网络系统-";
    private static final int DEFAULT_NETWORK_COLOR = 0xFF6E859D;

    private final Map<NetworkKey, NBTTagCompound> networks = new HashMap<>();
    private final Map<NetworkKey, String> networkDisplayNames = new HashMap<>();
    private final Map<NetworkKey, Integer> networkColors = new HashMap<>();
    private final Map<NetworkKey, UUID> networkOwners = new HashMap<>();
    private final Set<NetworkKey> pinnedNetworks = new HashSet<>();
    private final Map<NetworkPoolKey, NetworkResourcePool.ResourcePoolTotals> resourcePoolTotalsCache = new HashMap<>();
    private final Map<ControllerKey, ControllerSnapshot> controllerSnapshots = new HashMap<>();
    private final Map<Integer, Set<Long>> controllersByDimension = new HashMap<>();
    private final Map<NetworkKey, Set<Long>> controllersByNetwork = new HashMap<>();
    private int nextNetworkDisplayIndex = 1;

    public MMCENetworkSavedData() {
        super(DATA_NAME);
    }

    public MMCENetworkSavedData(final String name) {
        super(name);
    }

    public static MMCENetworkSavedData get(final World world) {
        MapStorage storage = world.getPerWorldStorage();
        if (storage == null) {
            return new MMCENetworkSavedData();
        }
        MMCENetworkSavedData data = loadData(storage);
        if (data == null) {
            data = new MMCENetworkSavedData();
            storeData(storage, data);
        }
        return data;
    }

    private static MMCENetworkSavedData loadData(final MapStorage storage) {
        try {
            Method method = MapStorage.class.getMethod("getOrLoadData", Class.class, String.class);
            Object value = method.invoke(storage, MMCENetworkSavedData.class, DATA_NAME);
            return value instanceof MMCENetworkSavedData ? (MMCENetworkSavedData) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = MapStorage.class.getMethod("func_75742_a", Class.class, String.class);
            Object value = method.invoke(storage, MMCENetworkSavedData.class, DATA_NAME);
            return value instanceof MMCENetworkSavedData ? (MMCENetworkSavedData) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        return null;
    }

    private static void storeData(final MapStorage storage, final MMCENetworkSavedData data) {
        try {
            Method method = MapStorage.class.getMethod("setData", String.class, WorldSavedData.class);
            method.invoke(storage, DATA_NAME, data);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = MapStorage.class.getMethod("func_75745_a", String.class, WorldSavedData.class);
            method.invoke(storage, DATA_NAME, data);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = MapStorage.class.getMethod("setData", String.class, Object.class);
            method.invoke(storage, DATA_NAME, data);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = MapStorage.class.getMethod("func_75745_a", String.class, Object.class);
            method.invoke(storage, DATA_NAME, data);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    public NBTTagCompound getNetworkData(final int dimension, final String networkId) {
        NBTTagCompound data = networks.get(new NetworkKey(dimension, networkId));
        return data == null ? new NBTTagCompound() : data.copy();
    }

    public NBTTagCompound getNetworkDataMutable(final int dimension, final String networkId) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        NBTTagCompound data = networks.get(key);
        if (data != null) {
            ensureDisplayName(key);
            return data;
        }

        data = new NBTTagCompound();
        networks.put(key, data);
        ensureDisplayName(key);
        markDirty();
        return data;
    }

    @Nullable
    public NBTTagCompound getExistingNetworkDataMutable(final int dimension, final String networkId) {
        return networks.get(new NetworkKey(dimension, networkId));
    }

    public boolean hasNetwork(final int dimension, final String networkId) {
        return networks.containsKey(new NetworkKey(dimension, networkId));
    }

    public NetworkResourcePool.ResourcePoolTotals getResourcePoolTotals(final int dimension, final String networkId, final String poolKey) {
        NetworkPoolKey key = new NetworkPoolKey(dimension, networkId, poolKey);
        NetworkResourcePool.ResourcePoolTotals cached = resourcePoolTotalsCache.get(key);
        if (cached != null) {
            return cached;
        }

        NetworkResourcePool.ResourcePoolTotals computed = NetworkResourcePool.getTotals(
            getExistingNetworkDataMutable(dimension, networkId),
            poolKey
        );
        resourcePoolTotalsCache.put(key, computed);
        return computed;
    }

    public void invalidateResourcePoolTotals(final int dimension, final String networkId, final String poolKey) {
        resourcePoolTotalsCache.remove(new NetworkPoolKey(dimension, networkId, poolKey));
    }

    public void invalidateAllResourcePoolTotals(final int dimension, final String networkId) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        resourcePoolTotalsCache.keySet().removeIf(poolKey -> poolKey.matches(key));
    }

    public void putNetworkData(final int dimension, final String networkId, final NBTTagCompound data) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        NBTTagCompound current = networks.get(key);
        if (current != null && current.equals(data)) {
            ensureDisplayName(key);
            return;
        }

        networks.put(key, data.copy());
        ensureDisplayName(key);
        invalidateAllResourcePoolTotals(dimension, networkId);
        markDirty();
    }

    public void registerNetwork(final int dimension, final String networkId) {
        registerNetwork(dimension, networkId, null);
    }

    public void registerNetwork(final int dimension, final String networkId, @Nullable final UUID owner) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        boolean existed = networks.containsKey(key);
        networks.computeIfAbsent(key, ignored -> new NBTTagCompound());
        if (!existed && owner != null) {
            networkOwners.put(key, owner);
        }
        ensureDisplayName(key);
        markDirty();
    }

    public void removeNetwork(final int dimension, final String networkId) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        if (networks.remove(key) != null) {
            networkDisplayNames.remove(key);
            networkColors.remove(key);
            networkOwners.remove(key);
            pinnedNetworks.remove(key);
            invalidateAllResourcePoolTotals(dimension, networkId);
            markDirty();
        }
    }

    public List<NetworkRef> getNetworkKeys(final int dimension) {
        List<NetworkRef> refs = new ArrayList<>();
        for (NetworkKey key : networks.keySet()) {
            if (key.dimension == dimension) {
                refs.add(new NetworkRef(key.dimension, key.networkId, ensureDisplayName(key), ensureColor(key), pinnedNetworks.contains(key), networkOwners.get(key)));
            }
        }
        refs.sort((left, right) -> {
            if (left.pinned != right.pinned) {
                return left.pinned ? -1 : 1;
            }
            return left.displayName.compareToIgnoreCase(right.displayName);
        });
        return refs;
    }

    @Nullable
    public UUID getNetworkOwner(final int dimension, final String networkId) {
        return networkOwners.get(new NetworkKey(dimension, networkId));
    }

    public boolean isNetworkPublic(final int dimension, final String networkId) {
        return !networkOwners.containsKey(new NetworkKey(dimension, networkId));
    }

    public String getNetworkDisplayName(final int dimension, final String networkId) {
        return ensureDisplayName(new NetworkKey(dimension, networkId));
    }

    public void setNetworkDisplayName(final int dimension, final String networkId, final String displayName) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        networks.computeIfAbsent(key, ignored -> new NBTTagCompound());
        String normalized = normalizeDisplayName(displayName);
        if (normalized.equals(networkDisplayNames.get(key))) {
            return;
        }
        networkDisplayNames.put(key, normalized);
        markDirty();
    }

    public int getNetworkColor(final int dimension, final String networkId) {
        return ensureColor(new NetworkKey(dimension, networkId));
    }

    public void setNetworkColor(final int dimension, final String networkId, final int color) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        networks.computeIfAbsent(key, ignored -> new NBTTagCompound());
        int normalized = normalizeColor(color);
        if (normalized == ensureColor(key)) {
            return;
        }
        networkColors.put(key, normalized);
        markDirty();
    }

    public boolean isNetworkPinned(final int dimension, final String networkId) {
        return pinnedNetworks.contains(new NetworkKey(dimension, networkId));
    }

    public void setNetworkPinned(final int dimension, final String networkId, final boolean pinned) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        networks.computeIfAbsent(key, ignored -> new NBTTagCompound());
        boolean changed = pinned ? pinnedNetworks.add(key) : pinnedNetworks.remove(key);
        if (changed) {
            markDirty();
        }
    }

    public ControllerSnapshot getControllerSnapshot(final int dimension, final long pos) {
        ControllerSnapshot snapshot = controllerSnapshots.get(new ControllerKey(dimension, pos));
        return snapshot == null ? null : snapshot.copy();
    }

    public void putControllerSnapshot(final int dimension, final long pos, final String networkId, final NBTTagCompound sharedData) {
        ControllerKey key = new ControllerKey(dimension, pos);
        ControllerSnapshot snapshot = new ControllerSnapshot(networkId, sharedData);
        ControllerSnapshot current = controllerSnapshots.get(key);
        if (snapshot.equals(current)) {
            return;
        }

        if (current != null) {
            removeControllerIndex(dimension, pos, current.networkId);
        }
        controllerSnapshots.put(key, snapshot);
        addControllerIndex(dimension, pos, networkId);
        markDirty();
    }

    public void removeControllerSnapshot(final int dimension, final long pos) {
        ControllerSnapshot removed = controllerSnapshots.remove(new ControllerKey(dimension, pos));
        if (removed != null) {
            removeControllerIndex(dimension, pos, removed.networkId);
            markDirty();
        }
    }

    public List<Long> getControllerPositions(final int dimension, final String networkId) {
        Set<Long> positions = controllersByNetwork.get(new NetworkKey(dimension, networkId));
        return positions == null ? Collections.emptyList() : new ArrayList<>(positions);
    }

    public List<Long> getAllControllerPositions(final int dimension) {
        Set<Long> positions = controllersByDimension.get(dimension);
        return positions == null ? Collections.emptyList() : new ArrayList<>(positions);
    }

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        networks.clear();
        networkDisplayNames.clear();
        networkColors.clear();
        networkOwners.clear();
        pinnedNetworks.clear();
        resourcePoolTotalsCache.clear();
        controllerSnapshots.clear();
        controllersByDimension.clear();
        controllersByNetwork.clear();
        nextNetworkDisplayIndex = Math.max(1, nbt.getInteger("nextNetworkDisplayIndex"));

        NBTTagList networkList = nbt.getTagList("networks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < networkList.tagCount(); i++) {
            NBTTagCompound entry = networkList.getCompoundTagAt(i);
            if (!entry.hasKey("networkId", Constants.NBT.TAG_STRING) || !entry.hasKey("data", Constants.NBT.TAG_COMPOUND)) {
                continue;
            }

            networks.put(
                new NetworkKey(entry.getInteger("dimension"), entry.getString("networkId")),
                entry.getCompoundTag("data").copy()
            );
        }

        NBTTagList displayNameList = nbt.getTagList("networkDisplayNames", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < displayNameList.tagCount(); i++) {
            NBTTagCompound entry = displayNameList.getCompoundTagAt(i);
            if (!entry.hasKey("networkId", Constants.NBT.TAG_STRING) || !entry.hasKey("displayName", Constants.NBT.TAG_STRING)) {
                continue;
            }

            NetworkKey key = new NetworkKey(entry.getInteger("dimension"), entry.getString("networkId"));
            networkDisplayNames.put(key, normalizeDisplayName(entry.getString("displayName")));
        }

        NBTTagList colorList = nbt.getTagList("networkColors", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < colorList.tagCount(); i++) {
            NBTTagCompound entry = colorList.getCompoundTagAt(i);
            if (!entry.hasKey("networkId", Constants.NBT.TAG_STRING)) {
                continue;
            }
            NetworkKey key = new NetworkKey(entry.getInteger("dimension"), entry.getString("networkId"));
            networkColors.put(key, normalizeColor(entry.getInteger("color")));
        }

        NBTTagList pinnedList = nbt.getTagList("pinnedNetworks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < pinnedList.tagCount(); i++) {
            NBTTagCompound entry = pinnedList.getCompoundTagAt(i);
            if (!entry.hasKey("networkId", Constants.NBT.TAG_STRING)) {
                continue;
            }
            pinnedNetworks.add(new NetworkKey(entry.getInteger("dimension"), entry.getString("networkId")));
        }

        NBTTagList ownerList = nbt.getTagList("networkOwners", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < ownerList.tagCount(); i++) {
            NBTTagCompound entry = ownerList.getCompoundTagAt(i);
            if (!entry.hasKey("networkId", Constants.NBT.TAG_STRING) || !entry.hasUniqueId("owner")) {
                continue;
            }
            networkOwners.put(
                new NetworkKey(entry.getInteger("dimension"), entry.getString("networkId")),
                entry.getUniqueId("owner")
            );
        }

        NBTTagList snapshotList = nbt.getTagList("controllerSnapshots", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < snapshotList.tagCount(); i++) {
            NBTTagCompound entry = snapshotList.getCompoundTagAt(i);
            if (!entry.hasKey("networkId", Constants.NBT.TAG_STRING) || !entry.hasKey("data", Constants.NBT.TAG_COMPOUND)) {
                continue;
            }

            int dimension = entry.getInteger("dimension");
            long pos = entry.getLong("pos");
            String networkId = entry.getString("networkId");
            controllerSnapshots.put(
                new ControllerKey(dimension, pos),
                new ControllerSnapshot(networkId, entry.getCompoundTag("data"))
            );
            addControllerIndex(dimension, pos, networkId);
        }

        for (NetworkKey key : networks.keySet()) {
            ensureDisplayName(key);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        NBTTagList networkList = new NBTTagList();
        for (Map.Entry<NetworkKey, NBTTagCompound> entry : networks.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dimension", entry.getKey().dimension);
            tag.setString("networkId", entry.getKey().networkId);
            tag.setTag("data", entry.getValue().copy());
            networkList.appendTag(tag);
        }
        compound.setTag("networks", networkList);

        NBTTagList displayNameList = new NBTTagList();
        for (Map.Entry<NetworkKey, String> entry : networkDisplayNames.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dimension", entry.getKey().dimension);
            tag.setString("networkId", entry.getKey().networkId);
            tag.setString("displayName", normalizeDisplayName(entry.getValue()));
            displayNameList.appendTag(tag);
        }
        compound.setTag("networkDisplayNames", displayNameList);

        NBTTagList colorList = new NBTTagList();
        for (Map.Entry<NetworkKey, Integer> entry : networkColors.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dimension", entry.getKey().dimension);
            tag.setString("networkId", entry.getKey().networkId);
            tag.setInteger("color", normalizeColor(entry.getValue()));
            colorList.appendTag(tag);
        }
        compound.setTag("networkColors", colorList);

        NBTTagList pinnedList = new NBTTagList();
        for (NetworkKey key : pinnedNetworks) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dimension", key.dimension);
            tag.setString("networkId", key.networkId);
            pinnedList.appendTag(tag);
        }
        compound.setTag("pinnedNetworks", pinnedList);

        NBTTagList ownerList = new NBTTagList();
        for (Map.Entry<NetworkKey, UUID> entry : networkOwners.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dimension", entry.getKey().dimension);
            tag.setString("networkId", entry.getKey().networkId);
            tag.setUniqueId("owner", entry.getValue());
            ownerList.appendTag(tag);
        }
        compound.setTag("networkOwners", ownerList);

        compound.setInteger("nextNetworkDisplayIndex", Math.max(1, nextNetworkDisplayIndex));

        NBTTagList snapshotList = new NBTTagList();
        for (Map.Entry<ControllerKey, ControllerSnapshot> entry : controllerSnapshots.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dimension", entry.getKey().dimension);
            tag.setLong("pos", entry.getKey().pos);
            tag.setString("networkId", entry.getValue().networkId);
            tag.setTag("data", entry.getValue().sharedData.copy());
            snapshotList.appendTag(tag);
        }
        compound.setTag("controllerSnapshots", snapshotList);

        return compound;
    }

    private String ensureDisplayName(final NetworkKey key) {
        String current = networkDisplayNames.get(key);
        if (current != null && !current.trim().isEmpty()) {
            return current;
        }

        String generated = DEFAULT_NETWORK_NAME_PREFIX + Math.max(1, nextNetworkDisplayIndex++);
        networkDisplayNames.put(key, generated);
        markDirty();
        return generated;
    }

    private int ensureColor(final NetworkKey key) {
        Integer current = networkColors.get(key);
        if (current != null) {
            return normalizeColor(current);
        }
        networkColors.put(key, DEFAULT_NETWORK_COLOR);
        markDirty();
        return DEFAULT_NETWORK_COLOR;
    }

    private String normalizeDisplayName(@Nullable final String displayName) {
        if (displayName == null) {
            return DEFAULT_NETWORK_NAME_PREFIX + Math.max(1, nextNetworkDisplayIndex++);
        }
        String normalized = displayName.trim();
        return normalized.isEmpty() ? DEFAULT_NETWORK_NAME_PREFIX + Math.max(1, nextNetworkDisplayIndex++) : normalized;
    }

    private int normalizeColor(final int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }

    private void addControllerIndex(final int dimension, final long pos, final String networkId) {
        controllersByDimension.computeIfAbsent(dimension, ignored -> new HashSet<>()).add(pos);
        controllersByNetwork.computeIfAbsent(new NetworkKey(dimension, networkId), ignored -> new HashSet<>()).add(pos);
    }

    private void removeControllerIndex(final int dimension, final long pos, final String networkId) {
        Set<Long> dimensionSet = controllersByDimension.get(dimension);
        if (dimensionSet != null) {
            dimensionSet.remove(pos);
            if (dimensionSet.isEmpty()) {
                controllersByDimension.remove(dimension);
            }
        }

        NetworkKey networkKey = new NetworkKey(dimension, networkId);
        Set<Long> networkSet = controllersByNetwork.get(networkKey);
        if (networkSet != null) {
            networkSet.remove(pos);
            if (networkSet.isEmpty()) {
                controllersByNetwork.remove(networkKey);
            }
        }
    }

    public static final class ControllerSnapshot {
        private final String networkId;
        private final NBTTagCompound sharedData;

        private ControllerSnapshot(final String networkId, final NBTTagCompound sharedData) {
            this.networkId = networkId;
            this.sharedData = sharedData.copy();
        }

        public String getNetworkId() {
            return networkId;
        }

        public NBTTagCompound getSharedData() {
            return sharedData.copy();
        }

        public ControllerSnapshot copy() {
            return new ControllerSnapshot(networkId, sharedData);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ControllerSnapshot)) {
                return false;
            }
            ControllerSnapshot other = (ControllerSnapshot) obj;
            return networkId.equals(other.networkId) && sharedData.equals(other.sharedData);
        }

        @Override
        public int hashCode() {
            int result = networkId.hashCode();
            result = 31 * result + sharedData.hashCode();
            return result;
        }
    }

    public static final class NetworkRef {
        private final int dimension;
        private final String networkId;
        private final String displayName;
        private final int color;
        private final boolean pinned;
        @Nullable
        private final UUID owner;

        public NetworkRef(final int dimension, final String networkId, final String displayName, final int color, final boolean pinned, @Nullable final UUID owner) {
            this.dimension = dimension;
            this.networkId = networkId;
            this.displayName = displayName;
            this.color = color;
            this.pinned = pinned;
            this.owner = owner;
        }

        public int getDimension() {
            return dimension;
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

        @Nullable
        public UUID getOwner() {
            return owner;
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
            int result = dimension;
            result = 31 * result + networkId.hashCode();
            return result;
        }
    }

    private static final class ControllerKey {
        private final int dimension;
        private final long pos;

        private ControllerKey(final int dimension, final long pos) {
            this.dimension = dimension;
            this.pos = pos;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ControllerKey)) {
                return false;
            }
            ControllerKey other = (ControllerKey) obj;
            return dimension == other.dimension && pos == other.pos;
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + (int) (pos ^ (pos >>> 32));
            return result;
        }
    }

    private static final class NetworkPoolKey {
        private final int dimension;
        private final String networkId;
        private final String poolKey;

        private NetworkPoolKey(final int dimension, final String networkId, final String poolKey) {
            this.dimension = dimension;
            this.networkId = networkId;
            this.poolKey = poolKey;
        }

        private boolean matches(final NetworkKey key) {
            return dimension == key.dimension && networkId.equals(key.networkId);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NetworkPoolKey)) {
                return false;
            }
            NetworkPoolKey other = (NetworkPoolKey) obj;
            return dimension == other.dimension
                && networkId.equals(other.networkId)
                && poolKey.equals(other.poolKey);
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + networkId.hashCode();
            result = 31 * result + poolKey.hashCode();
            return result;
        }
    }
}

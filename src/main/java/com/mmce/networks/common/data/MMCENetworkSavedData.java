package com.mmce.networks.common.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class MMCENetworkSavedData extends WorldSavedData {
    private static final String DATA_NAME = "mmcenetworks_data";

    private final Map<NetworkKey, NBTTagCompound> networks = new HashMap<>();
    private final Map<ControllerKey, ControllerSnapshot> controllerSnapshots = new HashMap<>();

    public MMCENetworkSavedData() {
        super(DATA_NAME);
    }

    public MMCENetworkSavedData(final String name) {
        super(name);
    }

    public static MMCENetworkSavedData get(final World world) {
        MapStorage storage = world.getPerWorldStorage();
        MMCENetworkSavedData data = (MMCENetworkSavedData) storage.getOrLoadData(MMCENetworkSavedData.class, DATA_NAME);
        if (data == null) {
            data = new MMCENetworkSavedData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public NBTTagCompound getNetworkData(final int dimension, final String networkId) {
        NBTTagCompound data = networks.get(new NetworkKey(dimension, networkId));
        return data == null ? new NBTTagCompound() : data.copy();
    }

    public boolean hasNetwork(final int dimension, final String networkId) {
        return networks.containsKey(new NetworkKey(dimension, networkId));
    }

    public void putNetworkData(final int dimension, final String networkId, final NBTTagCompound data) {
        NetworkKey key = new NetworkKey(dimension, networkId);
        NBTTagCompound current = networks.get(key);
        if (current != null && current.equals(data)) {
            return;
        }

        networks.put(key, data.copy());
        markDirty();
    }

    public void removeNetwork(final int dimension, final String networkId) {
        if (networks.remove(new NetworkKey(dimension, networkId)) != null) {
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

        controllerSnapshots.put(key, snapshot);
        markDirty();
    }

    public void removeControllerSnapshot(final int dimension, final long pos) {
        if (controllerSnapshots.remove(new ControllerKey(dimension, pos)) != null) {
            markDirty();
        }
    }

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        networks.clear();
        controllerSnapshots.clear();

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

        NBTTagList snapshotList = nbt.getTagList("controllerSnapshots", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < snapshotList.tagCount(); i++) {
            NBTTagCompound entry = snapshotList.getCompoundTagAt(i);
            if (!entry.hasKey("networkId", Constants.NBT.TAG_STRING) || !entry.hasKey("data", Constants.NBT.TAG_COMPOUND)) {
                continue;
            }

            controllerSnapshots.put(
                new ControllerKey(entry.getInteger("dimension"), entry.getLong("pos")),
                new ControllerSnapshot(entry.getString("networkId"), entry.getCompoundTag("data"))
            );
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
}

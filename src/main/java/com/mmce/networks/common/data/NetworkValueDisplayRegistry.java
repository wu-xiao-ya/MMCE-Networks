package com.mmce.networks.common.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NetworkValueDisplayRegistry {
    private static final Object LOCK = new Object();
    private static final Map<String, ValueDisplaySpec> SPECS = new LinkedHashMap<>();

    private NetworkValueDisplayRegistry() {
    }

    public static void clear() {
        synchronized (LOCK) {
            SPECS.clear();
        }
    }

    public static void register(final String key, final String displayName, @Nullable final String template) {
        if (isNullOrEmpty(key) || isNullOrEmpty(displayName)) {
            return;
        }

        synchronized (LOCK) {
            SPECS.put(key, new ValueDisplaySpec(key, displayName, isNullOrEmpty(template) ? "{value}" : template));
        }
    }

    public static boolean remove(final String key) {
        if (isNullOrEmpty(key)) {
            return false;
        }

        synchronized (LOCK) {
            return SPECS.remove(key) != null;
        }
    }

    public static List<ValueDisplaySpec> getSpecs() {
        synchronized (LOCK) {
            return new ArrayList<>(SPECS.values());
        }
    }

    public static boolean hasSpecs() {
        synchronized (LOCK) {
            return !SPECS.isEmpty();
        }
    }

    public static NBTTagCompound toNbt() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList entries = new NBTTagList();
        synchronized (LOCK) {
            for (ValueDisplaySpec spec : SPECS.values()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("key", spec.key);
                entry.setString("displayName", spec.displayName);
                entry.setString("template", spec.template);
                entries.appendTag(entry);
            }
        }
        root.setTag("entries", entries);
        return root;
    }

    public static List<ValueDisplaySpec> fromNbt(@Nullable final NBTTagCompound root) {
        List<ValueDisplaySpec> result = new ArrayList<>();
        if (root == null || !root.hasKey("entries", Constants.NBT.TAG_LIST)) {
            return result;
        }

        NBTTagList entries = root.getTagList("entries", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            String key = entry.getString("key");
            String displayName = entry.getString("displayName");
            String template = entry.getString("template");
            if (isNullOrEmpty(key) || isNullOrEmpty(displayName)) {
                continue;
            }
            result.add(new ValueDisplaySpec(key, displayName, isNullOrEmpty(template) ? "{value}" : template));
        }
        return result;
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }

    public static final class ValueDisplaySpec {
        private final String key;
        private final String displayName;
        private final String template;

        private ValueDisplaySpec(final String key, final String displayName, final String template) {
            this.key = key;
            this.displayName = displayName;
            this.template = template;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTemplate() {
            return template;
        }
    }
}

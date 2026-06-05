package com.mmce.networks.common.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NetworkValueDisplayRegistry {
    private static final Object LOCK = new Object();
    private static final Map<String, ValueDisplaySpec> SPECS = new LinkedHashMap<>();
    private static final String DEFAULT_LAYOUT = "list";
    private static String layout = DEFAULT_LAYOUT;

    private NetworkValueDisplayRegistry() {
    }

    public static void clear() {
        synchronized (LOCK) {
            SPECS.clear();
            layout = DEFAULT_LAYOUT;
        }
    }

    public static void register(final String key, final String displayName, @Nullable final String template) {
        register(key, displayName, template, "value", "");
    }

    public static void register(
        final String key,
        final String displayName,
        @Nullable final String template,
        @Nullable final String cardType,
        @Nullable final String options
    ) {
        if (isNullOrEmpty(key) || isNullOrEmpty(displayName)) {
            return;
        }

        synchronized (LOCK) {
            SPECS.put(key, new ValueDisplaySpec(
                key,
                displayName,
                isNullOrEmpty(template) ? "{value}" : template,
                normalizeCardType(cardType),
                options == null ? "" : options
            ));
        }
    }

    public static void setLayout(@Nullable final String nextLayout) {
        synchronized (LOCK) {
            layout = normalizeLayout(nextLayout);
        }
    }

    public static String getLayout() {
        synchronized (LOCK) {
            return layout;
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
            root.setString("layout", layout);
            for (ValueDisplaySpec spec : SPECS.values()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("key", spec.key);
                entry.setString("displayName", spec.displayName);
                entry.setString("template", spec.template);
                entry.setString("cardType", spec.cardType);
                entry.setString("options", spec.options);
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
            String cardType = entry.getString("cardType");
            String options = entry.getString("options");
            if (isNullOrEmpty(key) || isNullOrEmpty(displayName)) {
                continue;
            }
            result.add(new ValueDisplaySpec(
                key,
                displayName,
                isNullOrEmpty(template) ? "{value}" : template,
                normalizeCardType(cardType),
                options == null ? "" : options
            ));
        }
        return result;
    }

    public static String layoutFromNbt(@Nullable final NBTTagCompound root) {
        return root == null ? DEFAULT_LAYOUT : normalizeLayout(root.getString("layout"));
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }

    private static String normalizeCardType(@Nullable final String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("text".equals(normalized) || "bar".equals(normalized) || "status".equals(normalized)) {
            return normalized;
        }
        return "value";
    }

    private static String normalizeLayout(@Nullable final String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("dashboard".equals(normalized) || "story".equals(normalized) || "machine".equals(normalized)) {
            return normalized;
        }
        return DEFAULT_LAYOUT;
    }

    public static final class ValueDisplaySpec {
        private final String key;
        private final String displayName;
        private final String template;
        private final String cardType;
        private final String options;

        private ValueDisplaySpec(final String key, final String displayName, final String template, final String cardType, final String options) {
            this.key = key;
            this.displayName = displayName;
            this.template = template;
            this.cardType = cardType;
            this.options = options;
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

        public String getCardType() {
            return cardType;
        }

        public String getOptions() {
            return options;
        }
    }
}

package com.mmce.networks.common.data;

import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.Set;

public final class NetworkTechTree {
    private static final String ROOT_TAG = "_techTree";
    private static final String TECHS_TAG = "techs";
    private static final String REQUIRES_TAG = "requires";
    private static final String UNLOCKED_TAG = "unlocked";

    private NetworkTechTree() {
    }

    public static boolean defineTech(final NBTTagCompound sharedData, final String techId) {
        if (isNullOrEmpty(techId)) {
            return false;
        }
        return getTechDefinition(sharedData, techId, true) != null;
    }

    public static boolean removeTech(final NBTTagCompound sharedData, final String techId) {
        if (isNullOrEmpty(techId)) {
            return false;
        }

        NBTTagCompound techs = getTechs(sharedData, false);
        if (techs == null || !techs.hasKey(techId, 10)) {
            return false;
        }

        techs.removeTag(techId);
        removeUnlockState(sharedData, techId);
        cleanupTree(sharedData);
        return true;
    }

    public static boolean hasDefinition(final NBTTagCompound sharedData, final String techId) {
        NBTTagCompound techs = getTechs(sharedData, false);
        return techs != null && techs.hasKey(techId, 10);
    }

    public static boolean addPrerequisite(final NBTTagCompound sharedData, final String techId, final String prerequisiteId) {
        if (isNullOrEmpty(techId) || isNullOrEmpty(prerequisiteId) || techId.equals(prerequisiteId)) {
            return false;
        }

        NBTTagCompound tech = getTechDefinition(sharedData, techId, true);
        if (tech == null) {
            return false;
        }
        defineTech(sharedData, prerequisiteId);

        NBTTagCompound requires = getRequires(tech, true);
        if (requires == null) {
            return false;
        }
        requires.setTag(prerequisiteId, new NBTTagByte((byte) 1));
        return true;
    }

    public static boolean removePrerequisite(final NBTTagCompound sharedData, final String techId, final String prerequisiteId) {
        NBTTagCompound tech = getTechDefinition(sharedData, techId, false);
        if (tech == null) {
            return false;
        }

        NBTTagCompound requires = getRequires(tech, false);
        if (requires == null || !requires.hasKey(prerequisiteId)) {
            return false;
        }

        requires.removeTag(prerequisiteId);
        cleanupTechDefinition(tech);
        cleanupTree(sharedData);
        return true;
    }

    public static boolean isUnlocked(final NBTTagCompound sharedData, final String techId) {
        NBTTagCompound unlocked = getUnlocked(sharedData, false);
        return unlocked != null && unlocked.getBoolean(techId);
    }

    public static boolean canUnlock(final NBTTagCompound sharedData, final String techId) {
        if (!hasDefinition(sharedData, techId)) {
            return false;
        }

        NBTTagCompound tech = getTechDefinition(sharedData, techId, false);
        NBTTagCompound requires = tech == null ? null : getRequires(tech, false);
        if (requires == null) {
            return true;
        }

        for (String prerequisiteId : requires.getKeySet()) {
            if (!isUnlocked(sharedData, prerequisiteId)) {
                return false;
            }
        }
        return true;
    }

    public static boolean unlock(final NBTTagCompound sharedData, final String techId) {
        if (!canUnlock(sharedData, techId)) {
            return false;
        }

        NBTTagCompound unlocked = getUnlocked(sharedData, true);
        if (unlocked == null) {
            return false;
        }
        unlocked.setBoolean(techId, true);
        return true;
    }

    public static boolean lock(final NBTTagCompound sharedData, final String techId) {
        if (!hasDefinition(sharedData, techId)) {
            return false;
        }

        removeUnlockState(sharedData, techId);
        cleanupTree(sharedData);
        return true;
    }

    @Nullable
    public static NBTTagCompound getTechSnapshot(final NBTTagCompound sharedData, final String techId) {
        if (!hasDefinition(sharedData, techId)) {
            return null;
        }

        NBTTagCompound snapshot = new NBTTagCompound();
        snapshot.setString("id", techId);
        snapshot.setBoolean("defined", true);
        snapshot.setBoolean("unlocked", isUnlocked(sharedData, techId));
        snapshot.setBoolean("canUnlock", canUnlock(sharedData, techId));

        NBTTagCompound tech = getTechDefinition(sharedData, techId, false);
        NBTTagCompound requires = tech == null ? null : getRequires(tech, false);
        snapshot.setTag(REQUIRES_TAG, requires == null ? new NBTTagCompound() : requires.copy());
        return snapshot;
    }

    public static NBTTagCompound getTreeSnapshot(final NBTTagCompound sharedData) {
        NBTTagCompound snapshot = new NBTTagCompound();
        NBTTagCompound techs = getTechs(sharedData, false);
        if (techs == null) {
            return snapshot;
        }

        Set<String> techIds = techs.getKeySet();
        for (String techId : techIds) {
            NBTTagCompound techSnapshot = getTechSnapshot(sharedData, techId);
            if (techSnapshot != null) {
                snapshot.setTag(techId, techSnapshot);
            }
        }
        return snapshot;
    }

    @Nullable
    private static NBTTagCompound getTree(final NBTTagCompound sharedData, final boolean create) {
        if (sharedData == null) {
            return null;
        }
        if (sharedData.hasKey(ROOT_TAG, 10)) {
            return sharedData.getCompoundTag(ROOT_TAG);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound tree = new NBTTagCompound();
        sharedData.setTag(ROOT_TAG, tree);
        return tree;
    }

    @Nullable
    private static NBTTagCompound getTechs(final NBTTagCompound sharedData, final boolean create) {
        NBTTagCompound tree = getTree(sharedData, create);
        if (tree == null) {
            return null;
        }
        if (tree.hasKey(TECHS_TAG, 10)) {
            return tree.getCompoundTag(TECHS_TAG);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound techs = new NBTTagCompound();
        tree.setTag(TECHS_TAG, techs);
        return techs;
    }

    @Nullable
    private static NBTTagCompound getUnlocked(final NBTTagCompound sharedData, final boolean create) {
        NBTTagCompound tree = getTree(sharedData, create);
        if (tree == null) {
            return null;
        }
        if (tree.hasKey(UNLOCKED_TAG, 10)) {
            return tree.getCompoundTag(UNLOCKED_TAG);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound unlocked = new NBTTagCompound();
        tree.setTag(UNLOCKED_TAG, unlocked);
        return unlocked;
    }

    @Nullable
    private static NBTTagCompound getTechDefinition(final NBTTagCompound sharedData, final String techId, final boolean create) {
        NBTTagCompound techs = getTechs(sharedData, create);
        if (techs == null || isNullOrEmpty(techId)) {
            return null;
        }
        if (techs.hasKey(techId, 10)) {
            return techs.getCompoundTag(techId);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound tech = new NBTTagCompound();
        techs.setTag(techId, tech);
        return tech;
    }

    @Nullable
    private static NBTTagCompound getRequires(final NBTTagCompound tech, final boolean create) {
        if (tech == null) {
            return null;
        }
        if (tech.hasKey(REQUIRES_TAG, 10)) {
            return tech.getCompoundTag(REQUIRES_TAG);
        }
        if (!create) {
            return null;
        }

        NBTTagCompound requires = new NBTTagCompound();
        tech.setTag(REQUIRES_TAG, requires);
        return requires;
    }

    private static void removeUnlockState(final NBTTagCompound sharedData, final String techId) {
        NBTTagCompound unlocked = getUnlocked(sharedData, false);
        if (unlocked != null) {
            unlocked.removeTag(techId);
        }
    }

    private static void cleanupTechDefinition(final NBTTagCompound tech) {
        NBTTagCompound requires = getRequires(tech, false);
        if (requires != null && requires.getKeySet().isEmpty()) {
            tech.removeTag(REQUIRES_TAG);
        }
    }

    private static void cleanupTree(final NBTTagCompound sharedData) {
        NBTTagCompound tree = getTree(sharedData, false);
        if (tree == null) {
            return;
        }

        NBTTagCompound techs = getTechs(sharedData, false);
        if (techs != null && techs.getKeySet().isEmpty()) {
            tree.removeTag(TECHS_TAG);
        }

        NBTTagCompound unlocked = getUnlocked(sharedData, false);
        if (unlocked != null && unlocked.getKeySet().isEmpty()) {
            tree.removeTag(UNLOCKED_TAG);
        }

        if (tree.getKeySet().isEmpty()) {
            sharedData.removeTag(ROOT_TAG);
        }
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }
}

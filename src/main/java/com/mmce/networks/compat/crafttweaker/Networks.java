package com.mmce.networks.compat.crafttweaker;

import com.mmce.networks.common.data.NetworkResourcePool;
import com.mmce.networks.common.data.NetworkTechTree;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.handler.ControllerNetworkSyncHandler;
import com.mmce.networks.common.config.MMCENetworksConfig;
import com.mmce.networks.common.handler.TransientSupplyScheduler;
import com.mmce.networks.api.MMCENetworkApi;
import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.util.WorldCompat;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeEvent;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nullable;
@ZenRegister
@ZenClass("mods.mmcenetworks.Networks")
public final class Networks {
    private static final MmceReflection REFLECTION = new MmceReflection();

    private Networks() {
    }

    @ZenMethod
    public static boolean hasNetwork(final IMachineController controller) {
        return !isNullOrEmpty(getNetworkId(controller));
    }

    @Nullable
    @ZenMethod
    public static String getNetworkId(final IMachineController controller) {
        TileEntity tile = asTile(controller);
        return tile == null ? null : REFLECTION.getBoundNetworkId(tile);
    }

    @ZenMethod
    public static IData getData(final IMachineController controller) {
        TileEntity tile = asTile(controller);
        NetworkContext context = getContext(controller);
        if (context != null) {
            return CraftTweakerMC.getIDataModifyable(context.getSharedData());
        }
        return CraftTweakerMC.getIDataModifyable(tile == null ? new NBTTagCompound() : REFLECTION.getSharedData(tile));
    }

    @ZenMethod
    public static boolean setData(final IMachineController controller, final IData data) {
        return setSharedData(controller, CraftTweakerMC.getNBTCompound(data));
    }

    @ZenMethod
    public static boolean contains(final IMachineController controller, final String key) {
        return getTag(controller, key) != null;
    }

    @Nullable
    @ZenMethod
    public static IData get(final IMachineController controller, final String key) {
        NBTBase value = getTag(controller, key);
        return value == null ? null : CraftTweakerMC.getIData(value);
    }

    @ZenMethod
    public static boolean remove(final IMachineController controller, final String key) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            if (!sharedData.hasKey(key)) {
                return false;
            }

            sharedData.removeTag(key);
            return context.apply(sharedData);
        }
    }

    @ZenMethod
    public static boolean set(final IMachineController controller, final String key, final IData value) {
        return setTag(controller, key, CraftTweakerMC.getNBT(value));
    }

    @ZenMethod
    public static boolean set(final IMachineController controller, final String expression) {
        return ExpressionEngine.evaluate(controller, expression);
    }

    @ZenMethod
    public static int getInt(final IMachineController controller, final String key, final int defaultValue) {
        NBTBase value = getTag(controller, key);
        return value == null ? defaultValue : CraftTweakerMC.getIData(value).asInt();
    }

    @ZenMethod
    public static long getLong(final IMachineController controller, final String key, final long defaultValue) {
        NBTBase value = getTag(controller, key);
        return value == null ? defaultValue : CraftTweakerMC.getIData(value).asLong();
    }

    @ZenMethod
    public static double getDouble(final IMachineController controller, final String key, final double defaultValue) {
        NBTBase value = getTag(controller, key);
        return value == null ? defaultValue : CraftTweakerMC.getIData(value).asDouble();
    }

    @ZenMethod
    public static boolean getBoolean(final IMachineController controller, final String key, final boolean defaultValue) {
        NBTBase value = getTag(controller, key);
        return value == null ? defaultValue : CraftTweakerMC.getIData(value).asBool();
    }

    @Nullable
    @ZenMethod
    public static String getString(final IMachineController controller, final String key, @Nullable final String defaultValue) {
        NBTBase value = getTag(controller, key);
        return value == null ? defaultValue : CraftTweakerMC.getIData(value).asString();
    }

    @ZenMethod
    public static boolean setInt(final IMachineController controller, final String key, final int value) {
        return setTag(controller, key, new NBTTagInt(value));
    }

    @ZenMethod
    public static boolean setLong(final IMachineController controller, final String key, final long value) {
        return setTag(controller, key, new NBTTagLong(value));
    }

    @ZenMethod
    public static boolean setDouble(final IMachineController controller, final String key, final double value) {
        return setTag(controller, key, new NBTTagDouble(value));
    }

    @ZenMethod
    public static boolean setBoolean(final IMachineController controller, final String key, final boolean value) {
        return setTag(controller, key, new NBTTagByte((byte) (value ? 1 : 0)));
    }

    @ZenMethod
    public static boolean setString(final IMachineController controller, final String key, final String value) {
        return setTag(controller, key, new NBTTagString(value));
    }

    @ZenMethod
    public static int addInt(final IMachineController controller, final String key, final int delta) {
        return (int) updateNumeric(controller, key, delta, NumericType.INT, false);
    }

    @ZenMethod
    public static boolean tryConsumeInt(final IMachineController controller, final String key, final int amount) {
        if (amount < 0) {
            return false;
        }

        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            int currentValue = sharedData.hasKey(key) ? CraftTweakerMC.getIData(sharedData.getTag(key)).asInt() : 0;
            if (currentValue < amount) {
                return false;
            }

            sharedData.setInteger(key, currentValue - amount);
            return context.apply(sharedData);
        }
    }

    @ZenMethod
    public static long getCapacity(final IMachineController controller, final String key) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0L;
        }

        synchronized (context.getSavedData()) {
            return NetworkResourcePool.getTotalSupply(context.getMutableSharedData(), key);
        }
    }

    @ZenMethod
    public static long getUsed(final IMachineController controller, final String key) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0L;
        }

        synchronized (context.getSavedData()) {
            return NetworkResourcePool.getTotalUsage(context.getMutableSharedData(), key);
        }
    }

    @ZenMethod
    public static long getAvailable(final IMachineController controller, final String key) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0L;
        }

        synchronized (context.getSavedData()) {
            return NetworkResourcePool.getAvailable(context.getMutableSharedData(), key);
        }
    }

    @ZenMethod
    public static long getSupply(final IMachineController controller, final String key) {
        return getSupply(controller, key, null);
    }

    @ZenMethod
    public static long getSupply(final IMachineController controller, final String key, @Nullable final String source) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0L;
        }

        synchronized (context.getSavedData()) {
            return NetworkResourcePool.getSupply(context.getMutableSharedData(), key, context.scopeSource(source));
        }
    }

    @ZenMethod
    public static long getUsage(final IMachineController controller, final String key) {
        return getUsage(controller, key, null);
    }

    @ZenMethod
    public static long getUsage(final IMachineController controller, final String key, @Nullable final String source) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0L;
        }

        synchronized (context.getSavedData()) {
            return NetworkResourcePool.getUsage(context.getMutableSharedData(), key, context.scopeSource(source));
        }
    }

    @ZenMethod
    public static long setSupply(final IMachineController controller, final String key, final int amount) {
        return setSupply(controller, key, null, amount);
    }

    @ZenMethod
    public static long setSupply(final IMachineController controller, final String key, @Nullable final String source, final int amount) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0L;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            long total = NetworkResourcePool.setSupply(sharedData, key, context.scopeSource(source), amount);
            context.apply(sharedData);
            return total;
        }
    }

    @ZenMethod
    public static long clearSupply(final IMachineController controller, final String key) {
        return clearSupply(controller, key, null);
    }

    @ZenMethod
    public static long clearSupply(final IMachineController controller, final String key, @Nullable final String source) {
        return setSupply(controller, key, source, 0);
    }

    @ZenMethod
    public static long pulseSupply(final IMachineController controller, final String key, final int amount) {
        return pulseSupply(controller, key, null, amount);
    }

    @ZenMethod
    public static long pulseSupply(final IMachineController controller, final String key, @Nullable final String source, final int amount) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0L;
        }

        long expiresAt = System.currentTimeMillis() + Math.max(1L, MMCENetworksConfig.transientSupplyGraceTicks) * 50L;
        String scopedSource = context.scopeSource(source);
        long total = TransientSupplyScheduler.pulse(
            context.world,
            WorldCompat.getDimension(context.world),
            context.networkId,
            key,
            scopedSource,
            amount,
            expiresAt
        );
        TileEntity tile = asTile(controller);
        if (tile != null) {
            REFLECTION.markForUpdateSync(tile);
        }
        return total;
    }

    @ZenMethod
    public static long pulseThreadSupply(final FactoryRecipeEvent event, final String key, final int amount) {
        if (event == null || event.getFactoryRecipeThread() == null) {
            return 0L;
        }
        String threadName = event.getFactoryRecipeThread().getThreadName();
        String source = "thread:"
            + Integer.toHexString(System.identityHashCode(event.getFactoryRecipeThread()))
            + ":"
            + (threadName == null ? "unnamed" : threadName);
        return pulseSupply(event.getController(), key, source, amount);
    }

    @ZenMethod
    public static boolean trySetUsage(final IMachineController controller, final String key, final int amount) {
        return trySetUsage(controller, key, null, amount);
    }

    @ZenMethod
    public static boolean trySetUsage(final IMachineController controller, final String key, @Nullable final String source, final int amount) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            boolean updated = NetworkResourcePool.trySetUsage(sharedData, key, context.scopeSource(source), amount);
            return updated && context.apply(sharedData);
        }
    }

    @ZenMethod
    public static boolean tryAddUsage(final IMachineController controller, final String key, final int amount) {
        return tryAddUsage(controller, key, null, amount);
    }

    @ZenMethod
    public static boolean tryAddUsage(final IMachineController controller, final String key, @Nullable final String source, final int amount) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            boolean updated = NetworkResourcePool.tryAddUsage(sharedData, key, context.scopeSource(source), amount);
            return updated && context.apply(sharedData);
        }
    }

    @ZenMethod
    public static long releaseUsage(final IMachineController controller, final String key, final int amount) {
        return releaseUsage(controller, key, null, amount);
    }

    @ZenMethod
    public static long releaseUsage(final IMachineController controller, final String key, @Nullable final String source, final int amount) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0L;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            long remaining = NetworkResourcePool.releaseUsage(sharedData, key, context.scopeSource(source), amount);
            context.apply(sharedData);
            return remaining;
        }
    }

    @ZenMethod
    public static boolean clearUsage(final IMachineController controller, final String key) {
        return clearUsage(controller, key, null);
    }

    @ZenMethod
    public static boolean clearUsage(final IMachineController controller, final String key, @Nullable final String source) {
        return trySetUsage(controller, key, source, 0);
    }

    @ZenMethod
    public static boolean canUse(final IMachineController controller, final String key, final int amount) {
        if (amount < 0) {
            return false;
        }
        return getAvailable(controller, key) >= amount;
    }

    @ZenMethod
    public static IData getAllResourcePools(final IMachineController controller) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return CraftTweakerMC.getIDataModifyable(new NBTTagCompound());
        }

        synchronized (context.getSavedData()) {
            return CraftTweakerMC.getIDataModifyable(NetworkResourcePool.getAllPoolsSnapshot(context.getMutableSharedData()));
        }
    }

    @ZenMethod
    public static IData getResourcePool(final IMachineController controller, final String key) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return CraftTweakerMC.getIDataModifyable(new NBTTagCompound());
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound snapshot = NetworkResourcePool.getPoolSnapshot(context.getMutableSharedData(), key);
            return CraftTweakerMC.getIDataModifyable(snapshot == null ? new NBTTagCompound() : snapshot);
        }
    }

    @ZenMethod
    public static boolean defineTech(final IMachineController controller, final String techId) {
        return mutateTech(controller, sharedData -> NetworkTechTree.defineTech(sharedData, techId));
    }

    @ZenMethod
    public static boolean removeTech(final IMachineController controller, final String techId) {
        return mutateTech(controller, sharedData -> NetworkTechTree.removeTech(sharedData, techId));
    }

    @ZenMethod
    public static boolean addTechPrerequisite(final IMachineController controller, final String techId, final String prerequisiteId) {
        return mutateTech(controller, sharedData -> NetworkTechTree.addPrerequisite(sharedData, techId, prerequisiteId));
    }

    @ZenMethod
    public static boolean removeTechPrerequisite(final IMachineController controller, final String techId, final String prerequisiteId) {
        return mutateTech(controller, sharedData -> NetworkTechTree.removePrerequisite(sharedData, techId, prerequisiteId));
    }

    @ZenMethod
    public static boolean hasTech(final IMachineController controller, final String techId) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            return NetworkTechTree.hasDefinition(context.getMutableSharedData(), techId);
        }
    }

    @ZenMethod
    public static boolean isTechUnlocked(final IMachineController controller, final String techId) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            return NetworkTechTree.isUnlocked(context.getMutableSharedData(), techId);
        }
    }

    @ZenMethod
    public static boolean canUnlockTech(final IMachineController controller, final String techId) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            return NetworkTechTree.canUnlock(context.getMutableSharedData(), techId);
        }
    }

    @ZenMethod
    public static boolean unlockTech(final IMachineController controller, final String techId) {
        return mutateTech(controller, sharedData -> NetworkTechTree.unlock(sharedData, techId));
    }

    @ZenMethod
    public static boolean lockTech(final IMachineController controller, final String techId) {
        return mutateTech(controller, sharedData -> NetworkTechTree.lock(sharedData, techId));
    }

    @ZenMethod
    public static IData getTechTree(final IMachineController controller) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return CraftTweakerMC.getIDataModifyable(new NBTTagCompound());
        }

        synchronized (context.getSavedData()) {
            return CraftTweakerMC.getIDataModifyable(NetworkTechTree.getTreeSnapshot(context.getMutableSharedData()));
        }
    }

    @ZenMethod
    public static IData getTech(final IMachineController controller, final String techId) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return CraftTweakerMC.getIDataModifyable(new NBTTagCompound());
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound snapshot = NetworkTechTree.getTechSnapshot(context.getMutableSharedData(), techId);
            return CraftTweakerMC.getIDataModifyable(snapshot == null ? new NBTTagCompound() : snapshot);
        }
    }

    @ZenMethod
    public static boolean eval(final IMachineController controller, final String expression) {
        return ExpressionEngine.evaluate(controller, expression);
    }

    private static boolean setTag(final IMachineController controller, final String key, final NBTBase value) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            sharedData.setTag(key, value.copy());
            return context.apply(sharedData);
        }
    }

    private static double updateNumeric(final IMachineController controller, final String key, final double delta, final NumericType type, final boolean overwrite) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return 0.0D;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            double currentValue = sharedData.hasKey(key) ? readNumeric(sharedData.getTag(key)) : 0.0D;
            double nextValue = overwrite ? delta : currentValue + delta;
            writeNumeric(sharedData, key, nextValue, type);
            context.apply(sharedData);
            return nextValue;
        }
    }

    private static boolean mutateTech(final IMachineController controller, final TechMutation mutation) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }

        synchronized (context.getSavedData()) {
            NBTTagCompound sharedData = context.getMutableSharedData();
            boolean changed = mutation.apply(sharedData);
            return changed && context.apply(sharedData);
        }
    }

    @Nullable
    private static NBTBase getTag(final IMachineController controller, final String key) {
        TileEntity tile = asTile(controller);
        NetworkContext context = getContext(controller);
        NBTTagCompound sharedData;
        if (context != null) {
            synchronized (context.getSavedData()) {
                sharedData = context.getMutableSharedData();
            }
        } else if (tile != null) {
            sharedData = REFLECTION.getSharedData(tile);
        } else {
            return null;
        }
        return sharedData.hasKey(key) ? sharedData.getTag(key).copy() : null;
    }

    private static boolean setSharedData(final IMachineController controller, final NBTTagCompound sharedData) {
        NetworkContext context = getContext(controller);
        if (context == null) {
            return false;
        }
        synchronized (context.getSavedData()) {
            return context.apply(sharedData);
        }
    }

    @Nullable
    private static TileEntity asTile(@Nullable final IMachineController controller) {
        return controller instanceof TileEntity ? (TileEntity) controller : null;
    }

    @Nullable
    private static NetworkContext getContext(@Nullable final IMachineController controller) {
        TileEntity tile = asTile(controller);
        if (tile == null) {
            return null;
        }

        World world = tile.getWorld();
        String networkId = REFLECTION.getBoundNetworkId(tile);
        if (world == null || WorldCompat.isRemote(world) || isNullOrEmpty(networkId)) {
            return null;
        }

        return new NetworkContext(world, tile, networkId);
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }

    private static double readNumeric(final NBTBase tag) {
        return CraftTweakerMC.getIData(tag).asDouble();
    }

    private static void writeNumeric(final NBTTagCompound data, final String key, final double value, final NumericType type) {
        if (type == NumericType.INT) {
            data.setInteger(key, (int) value);
        } else if (type == NumericType.LONG) {
            data.setLong(key, (long) value);
        } else {
            data.setDouble(key, value);
        }
    }

    private enum NumericType {
        INT,
        LONG,
        DOUBLE
    }

    private interface TechMutation {
        boolean apply(NBTTagCompound sharedData);
    }

    private static final class ExpressionEngine {
        private ExpressionEngine() {
        }

        private static boolean evaluate(final IMachineController controller, final String expression) {
            NetworkContext context = getContext(controller);
            if (context == null) {
                return false;
            }

            synchronized (context.getSavedData()) {
                String[] statements = expression.split(";");
                NBTTagCompound sharedData = context.getMutableSharedData();
                for (String rawStatement : statements) {
                    String statement = rawStatement.trim();
                    if (statement.isEmpty()) {
                        continue;
                    }

                    int assignIndex = findAssignment(statement);
                    if (assignIndex < 0) {
                        return false;
                    }

                    String target = statement.substring(0, assignIndex).trim();
                    String expr = statement.substring(assignIndex + 1).trim();
                    if (!isIdentifier(target) || expr.isEmpty()) {
                        return false;
                    }

                    double value = new Parser(sharedData, expr).parseExpression();
                    if (Double.isNaN(value) || Double.isInfinite(value)) {
                        return false;
                    }

                    if (!ensureNumericTarget(sharedData, target, value)) {
                        return false;
                    }
                    writeParsedValue(sharedData, target, value, inferType(sharedData, target, value));
                }

                return context.apply(sharedData);
            }
        }

        private static int findAssignment(final String statement) {
            int depth = 0;
            for (int i = 0; i < statement.length(); i++) {
                char c = statement.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth = Math.max(0, depth - 1);
                } else if (c == '=' && depth == 0) {
                    if (i + 1 < statement.length() && statement.charAt(i + 1) == '=') {
                        return -1;
                    }
                    return i;
                }
            }
            return -1;
        }

        private static boolean isIdentifier(final String value) {
            if (value.isEmpty() || !Character.isJavaIdentifierStart(value.charAt(0))) {
                return false;
            }
            for (int i = 1; i < value.length(); i++) {
                if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        private static NumericType inferType(final NBTTagCompound data, final String key, final double value) {
            if (!data.hasKey(key)) {
                return value == (int) value ? NumericType.INT : NumericType.DOUBLE;
            }
            NBTBase tag = data.getTag(key);
            if (tag instanceof NBTTagLong) {
                return NumericType.LONG;
            }
            if (tag instanceof NBTTagDouble) {
                return NumericType.DOUBLE;
            }
            return NumericType.INT;
        }

        private static boolean ensureNumericTarget(final NBTTagCompound data, final String key, final double value) {
            if (!data.hasKey(key)) {
                return true;
            }
            NBTBase tag = data.getTag(key);
            return tag instanceof NBTTagInt || tag instanceof NBTTagLong || tag instanceof NBTTagDouble || tag instanceof NBTTagByte;
        }

        private static void writeParsedValue(final NBTTagCompound data, final String key, final double value, final NumericType type) {
            writeNumeric(data, key, value, type);
        }

        private static final class Parser {
            private final NBTTagCompound data;
            private final String input;
            private int index;

            private Parser(final NBTTagCompound data, final String input) {
                this.data = data;
                this.input = input;
            }

            private double parseExpression() {
                double value = parseTerm();
                while (true) {
                    skipWhitespace();
                    if (match('+')) {
                        value += parseTerm();
                    } else if (match('-')) {
                        value -= parseTerm();
                    } else {
                        return value;
                    }
                }
            }

            private double parseTerm() {
                double value = parseFactor();
                while (true) {
                    skipWhitespace();
                    if (match('*')) {
                        value *= parseFactor();
                    } else if (match('/')) {
                        value /= parseFactor();
                    } else if (match('%')) {
                        value %= parseFactor();
                    } else {
                        return value;
                    }
                }
            }

            private double parseFactor() {
                skipWhitespace();
                if (match('+')) {
                    return parseFactor();
                }
                if (match('-')) {
                    return -parseFactor();
                }
                if (match('(')) {
                    double value = parseExpression();
                    expect(')');
                    return value;
                }
                if (peekDigit() || peek('.')) {
                    return parseNumber();
                }
                return parseVariable();
            }

            private double parseNumber() {
                int start = index;
                while (index < input.length()) {
                    char c = input.charAt(index);
                    if (Character.isDigit(c) || c == '.') {
                        index++;
                    } else {
                        break;
                    }
                }
                return Double.parseDouble(input.substring(start, index));
            }

            private double parseVariable() {
                int start = index;
                if (index < input.length() && Character.isJavaIdentifierStart(input.charAt(index))) {
                    index++;
                    while (index < input.length() && Character.isJavaIdentifierPart(input.charAt(index))) {
                        index++;
                    }
                }
                String name = input.substring(start, index);
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Invalid expression");
                }
                NBTBase tag = data.getTag(name);
                return tag == null ? 0.0D : CraftTweakerMC.getIData(tag).asDouble();
            }

            private void skipWhitespace() {
                while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                    index++;
                }
            }

            private boolean match(final char c) {
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == c) {
                    index++;
                    return true;
                }
                return false;
            }

            private void expect(final char c) {
                if (!match(c)) {
                    throw new IllegalArgumentException("Expected '" + c + "'");
                }
            }

            private boolean peek(final char c) {
                skipWhitespace();
                return index < input.length() && input.charAt(index) == c;
            }

            private boolean peekDigit() {
                skipWhitespace();
                return index < input.length() && Character.isDigit(input.charAt(index));
            }
        }
    }

    private static final class NetworkContext {
        private final World world;
        private final TileEntity tile;
        private final String networkId;

        private NetworkContext(final World world, final TileEntity tile, final String networkId) {
            this.world = world;
            this.tile = tile;
            this.networkId = networkId;
        }

        private NBTTagCompound getSharedData() {
            return MMCENetworkApi.getSharedData(world, networkId);
        }

        private NBTTagCompound getMutableSharedData() {
            return getSavedData().getNetworkDataMutable(WorldCompat.getDimension(world), networkId);
        }

        private MMCENetworkSavedData getSavedData() {
            return MMCENetworkSavedData.get(world);
        }

        private boolean apply(final NBTTagCompound sharedData) {
            getSavedData().markDirty();
            if (!REFLECTION.setSharedData(tile, networkId, sharedData)) {
                return false;
            }

            REFLECTION.markForUpdateSync(tile);
            ControllerNetworkSyncHandler.markNetworkDirty(world, networkId);
            return true;
        }

        private String scopeSource(@Nullable final String source) {
            String base = WorldCompat.getDimension(world) + ":" + tile.getPos().toLong();
            return isNullOrEmpty(source) ? base : base + ":" + source;
        }
    }
}

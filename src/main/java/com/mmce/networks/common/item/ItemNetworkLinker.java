package com.mmce.networks.common.item;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.api.MMCENetworkApi;
import com.mmce.networks.common.data.NetworkAccess;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.compute.ComputeNetworkService;
import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.network.MessageOpenNetworkTerminal;
import com.mmce.networks.common.network.NetworkHandler;
import com.mmce.networks.common.util.ItemStackCompat;
import com.mmce.networks.common.util.PlayerCompat;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ItemNetworkLinker extends Item {
    private static final String TAG_NETWORK_ID = "networkId";
    private static final String TAG_MODE = "mode";
    private static final String TAG_LAST_ACTION = "lastActionTick";
    private static final String TRANSLATION_KEY = "mmcenetworks.network_linker";
    private static final long MESSAGE_DEDUP_WINDOW_MS = 1000L;
    private static final Map<String, Long> RECENT_MESSAGES = new HashMap<>();

    private final MmceReflection reflection = new MmceReflection();

    public ItemNetworkLinker() {
        setRegistryName(new ResourceLocation(MMCENetworksMod.MOD_ID, "network_linker"));
        applyTranslationKey(TRANSLATION_KEY);
        applyCreativeTab();
        applyMaxStackSize(1);
    }

    public String func_77658_a() {
        return TRANSLATION_KEY;
    }

    public String getUnlocalizedName() {
        return "item." + TRANSLATION_KEY;
    }

    public String func_77653_i(final ItemStack stack) {
        String localized = I18n.translateToLocal("item." + TRANSLATION_KEY + ".name");
        if (!("item." + TRANSLATION_KEY + ".name").equals(localized)) {
            return localized;
        }

        localized = I18n.translateToLocal("item." + TRANSLATION_KEY);
        if (!("item." + TRANSLATION_KEY).equals(localized)) {
            return localized;
        }

        localized = I18n.translateToLocal("item.network_linker.name");
        if (!"item.network_linker.name".equals(localized)) {
            return localized;
        }

        return "MMCE Network Linker";
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) {
            return new ActionResult<>(EnumActionResult.PASS, PlayerCompat.getHeldItem(player, hand));
        }

        ItemStack stack = PlayerCompat.getHeldItem(player, hand);
        if (stack == null) {
            return new ActionResult<>(EnumActionResult.PASS, new ItemStack(this));
        }
        if (!PlayerCompat.isSneaking(player)) {
            String networkId = getNetworkId(stack);
            if (!world.isRemote) {
                networkId = ensureNetworkForTerminal(player, world, stack, networkId);
                openTerminalForServerPlayer(player, networkId);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (!world.isRemote) {
            if (isDuplicateAction(stack, world)) {
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
            String networkId = createNetworkId();
            setNetworkId(stack, networkId);
            MMCENetworkApi.registerNetwork(world, networkId, player.getUniqueID());
            sendLinkerMessage(
                player,
                world,
                colorPair(TextFormatting.GREEN, "已创建网络: ", TextFormatting.AQUA, MMCENetworkApi.getNetworkDisplayName(world, networkId)),
                false
            );
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public EnumActionResult onItemUseFirst(
        final EntityPlayer player,
        final World world,
        final net.minecraft.util.math.BlockPos pos,
        final EnumFacing side,
        final float hitX,
        final float hitY,
        final float hitZ,
        final EnumHand hand
    ) {
        if (hand != EnumHand.MAIN_HAND) {
            return EnumActionResult.PASS;
        }
        if (!reflection.isAvailable()) {
            return EnumActionResult.PASS;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!reflection.isControllerTile(tile)) {
            return openTerminalFromBlockUse(player, world, hand);
        }

        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        ItemStack stack = PlayerCompat.getHeldItem(player, hand);
        if (stack == null) {
            return EnumActionResult.PASS;
        }
        if (isDuplicateAction(stack, world)) {
            return EnumActionResult.SUCCESS;
        }
        LinkerMode mode = getMode(stack);
        if (mode == LinkerMode.COPY) {
            copyControllerNetwork(player, stack, tile);
            return EnumActionResult.SUCCESS;
        }
        if (mode == LinkerMode.UNBIND) {
            unbindController(player, world, tile, pos.toLong());
            return EnumActionResult.SUCCESS;
        }
        if (mode == LinkerMode.INSPECT) {
            inspectController(player, tile);
            return EnumActionResult.SUCCESS;
        }

        String networkId = getNetworkId(stack);
        if (isNullOrEmpty(networkId)) {
            networkId = createNetworkId();
            setNetworkId(stack, networkId);
            MMCENetworkApi.registerNetwork(world, networkId, player.getUniqueID());
            sendLinkerMessage(
                player,
                world,
                colorPair(TextFormatting.GREEN, "已创建网络: ", TextFormatting.AQUA, MMCENetworkApi.getNetworkDisplayName(world, networkId)),
                false
            );
        }

        MMCENetworkSavedData data = MMCENetworkSavedData.get(world);
        int dimension = world.provider.getDimension();
        if (player instanceof EntityPlayerMP && !NetworkAccess.canAccess((EntityPlayerMP) player, data, dimension, networkId)) {
            sendLinkerMessage(player, world, new TextComponentString(TextFormatting.RED + "你没有权限访问这个网络"), false);
            return EnumActionResult.SUCCESS;
        }
        String previousNetworkId = reflection.getBoundNetworkId(tile);
        if (!isNullOrEmpty(previousNetworkId) && !previousNetworkId.equals(networkId)) {
            ComputeNetworkService.unbindRoute(
                world,
                previousNetworkId,
                dimension + ":" + pos.toLong()
            );
        }
        data.registerNetwork(dimension, networkId, player.getUniqueID());
        NBTTagCompound sharedData = data.getNetworkData(dimension, networkId);
        reflection.setSharedData(tile, networkId, sharedData);
        reflection.markForUpdateSync(tile);
        data.putControllerSnapshot(dimension, pos.toLong(), networkId, sharedData);
        sendLinkerMessage(
            player,
            world,
            colorPair(TextFormatting.GREEN, "控制器已绑定到网络: ", TextFormatting.AQUA, data.getNetworkDisplayName(dimension, networkId)),
            false
        );
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(
        final ItemStack stack,
        @Nullable final World worldIn,
        final List<String> tooltip,
        final ITooltipFlag flagIn
    ) {
        String networkId = getNetworkId(stack);
        tooltip.add(
            TextFormatting.DARK_AQUA + "网络 ID: "
                + (networkId == null ? TextFormatting.GRAY + "未写入网络 ID" : TextFormatting.WHITE + networkId)
        );
        tooltip.add(TextFormatting.GOLD + "模式: " + TextFormatting.YELLOW + getMode(stack).displayName);
        tooltip.add(TextFormatting.AQUA + "潜行" + TextFormatting.WHITE + " + " + TextFormatting.GOLD + "鼠标滚轮" + TextFormatting.WHITE + " : " + TextFormatting.YELLOW + "切换模式");
        tooltip.add(TextFormatting.GREEN + "右键空气" + TextFormatting.WHITE + " : " + TextFormatting.AQUA + "打开网络终端");
        tooltip.add(TextFormatting.BLUE + "潜行右键空气" + TextFormatting.WHITE + " : " + TextFormatting.GREEN + "生成新网络");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "右键 MMCE 控制器" + TextFormatting.WHITE + " : " + TextFormatting.YELLOW + "执行当前模式");
    }

    @Nullable
    public static String getNetworkId(final ItemStack stack) {
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        if (tag == null || !tag.hasKey(TAG_NETWORK_ID)) {
            return null;
        }
        return tag.getString(TAG_NETWORK_ID);
    }

    private static void setNetworkId(final ItemStack stack, final String networkId) {
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        if (tag == null) {
            tag = new NBTTagCompound();
        }
        tag.setString(TAG_NETWORK_ID, networkId);
        ItemStackCompat.setTagCompound(stack, tag);
    }

    public static void cycleMode(final ItemStack stack, final int direction) {
        LinkerMode[] modes = LinkerMode.values();
        int index = getMode(stack).ordinal() + (direction < 0 ? -1 : 1);
        if (index < 0) {
            index = modes.length - 1;
        } else if (index >= modes.length) {
            index = 0;
        }
        setMode(stack, modes[index]);
    }

    public static void sendModeMessage(final EntityPlayer player, final ItemStack stack) {
        sendLinkerMessage(player, player == null ? null : player.world, colorPair(TextFormatting.LIGHT_PURPLE, "网络绑定器模式: ", TextFormatting.YELLOW, getMode(stack).displayName), true);
    }

    private void copyControllerNetwork(final EntityPlayer player, final ItemStack stack, final TileEntity tile) {
        String boundNetworkId = reflection.getBoundNetworkId(tile);
        if (isNullOrEmpty(boundNetworkId)) {
            sendLinkerMessage(player, player == null ? null : player.world, new TextComponentString(TextFormatting.RED + "这个控制器还没有绑定网络"), false);
            return;
        }
        if (player instanceof EntityPlayerMP && player.world != null) {
            MMCENetworkSavedData data = MMCENetworkSavedData.get(player.world);
            if (!NetworkAccess.canAccess((EntityPlayerMP) player, data, player.world.provider.getDimension(), boundNetworkId)) {
                sendLinkerMessage(player, player.world, new TextComponentString(TextFormatting.RED + "你没有权限复制这个网络"), false);
                return;
            }
        }

        setNetworkId(stack, boundNetworkId);
        sendLinkerMessage(
            player,
            player == null ? null : player.world,
            colorPair(TextFormatting.GREEN, "已复制控制器网络: ", TextFormatting.AQUA, boundNetworkId),
            false
        );
    }

    private void unbindController(final EntityPlayer player, final World world, final TileEntity tile, final long pos) {
        String boundNetworkId = reflection.getBoundNetworkId(tile);
        if (isNullOrEmpty(boundNetworkId)) {
            sendLinkerMessage(player, world, new TextComponentString(TextFormatting.RED + "这个控制器还没有绑定网络"), false);
            return;
        }
        if (player instanceof EntityPlayerMP) {
            MMCENetworkSavedData data = MMCENetworkSavedData.get(world);
            if (!NetworkAccess.canAccess((EntityPlayerMP) player, data, world.provider.getDimension(), boundNetworkId)) {
                sendLinkerMessage(player, world, new TextComponentString(TextFormatting.RED + "你没有权限解绑这个网络"), false);
                return;
            }
        }

        ComputeNetworkService.unbindRoute(
            world,
            boundNetworkId,
            world.provider.getDimension() + ":" + pos
        );
        reflection.clearSharedData(tile);
        reflection.markForUpdateSync(tile);
        MMCENetworkSavedData.get(world).removeControllerSnapshot(world.provider.getDimension(), pos);
        sendLinkerMessage(player, world, colorPair(TextFormatting.GOLD, "已解绑控制器网络: ", TextFormatting.AQUA, boundNetworkId), false);
    }

    private void inspectController(final EntityPlayer player, final TileEntity tile) {
        String boundNetworkId = reflection.getBoundNetworkId(tile);
        if (!isNullOrEmpty(boundNetworkId) && player instanceof EntityPlayerMP && player.world != null) {
            MMCENetworkSavedData data = MMCENetworkSavedData.get(player.world);
            if (!NetworkAccess.canAccess((EntityPlayerMP) player, data, player.world.provider.getDimension(), boundNetworkId)) {
                sendLinkerMessage(player, player.world, new TextComponentString(TextFormatting.RED + "你没有权限查看这个网络"), false);
                return;
            }
        }
        sendLinkerMessage(
            player,
            player == null ? null : player.world,
            isNullOrEmpty(boundNetworkId)
                ? new TextComponentString(TextFormatting.RED + "这个控制器还没有绑定网络")
                : colorPair(TextFormatting.AQUA, "控制器网络: ", TextFormatting.WHITE, boundNetworkId),
            false
        );
    }

    private static LinkerMode getMode(final ItemStack stack) {
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        if (tag == null || !tag.hasKey(TAG_MODE)) {
            return LinkerMode.BIND;
        }
        int index = tag.getInteger(TAG_MODE);
        LinkerMode[] modes = LinkerMode.values();
        return index >= 0 && index < modes.length ? modes[index] : LinkerMode.BIND;
    }

    private static void setMode(final ItemStack stack, final LinkerMode mode) {
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        if (tag == null) {
            tag = new NBTTagCompound();
        }
        tag.setInteger(TAG_MODE, mode.ordinal());
        ItemStackCompat.setTagCompound(stack, tag);
    }

    private static String createNetworkId() {
        return "net_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String ensureNetworkForTerminal(
        final EntityPlayer player,
        final World world,
        final ItemStack stack,
        @Nullable final String currentNetworkId
    ) {
        if (!isNullOrEmpty(currentNetworkId)) {
            MMCENetworkSavedData data = MMCENetworkSavedData.get(world);
            if (player instanceof EntityPlayerMP && !NetworkAccess.canAccess((EntityPlayerMP) player, data, world.provider.getDimension(), currentNetworkId)) {
                sendLinkerMessage(player, world, new TextComponentString(TextFormatting.RED + "你没有权限访问这个网络"), false);
                return "";
            }
            MMCENetworkApi.registerNetwork(world, currentNetworkId, player.getUniqueID());
            return currentNetworkId;
        }

        String networkId = createNetworkId();
        setNetworkId(stack, networkId);
        MMCENetworkApi.registerNetwork(world, networkId, player.getUniqueID());
        sendLinkerMessage(
            player,
            world,
            colorPair(TextFormatting.GREEN, "已创建网络: ", TextFormatting.AQUA, MMCENetworkApi.getNetworkDisplayName(world, networkId)),
            false
        );
        return networkId;
    }

    private EnumActionResult openTerminalFromBlockUse(final EntityPlayer player, final World world, final EnumHand hand) {
        if (PlayerCompat.isSneaking(player)) {
            return EnumActionResult.PASS;
        }

        ItemStack stack = PlayerCompat.getHeldItem(player, hand);
        if (stack == null) {
            return EnumActionResult.PASS;
        }

        if (!world.isRemote) {
            String networkId = ensureNetworkForTerminal(player, world, stack, getNetworkId(stack));
            openTerminalForServerPlayer(player, networkId);
        }
        return EnumActionResult.SUCCESS;
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }

    private static boolean isDuplicateAction(final ItemStack stack, final World world) {
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        if (tag == null) {
            tag = new NBTTagCompound();
        }

        long tick = world.getTotalWorldTime();
        if (tag.getLong(TAG_LAST_ACTION) == tick) {
            return true;
        }

        tag.setLong(TAG_LAST_ACTION, tick);
        ItemStackCompat.setTagCompound(stack, tag);
        return false;
    }

    private static TextComponentString colorPair(
        final TextFormatting labelColor,
        final String label,
        final TextFormatting valueColor,
        final String value
    ) {
        return new TextComponentString(labelColor + label + valueColor + value);
    }

    private static void sendLinkerMessage(
        @Nullable final EntityPlayer player,
        @Nullable final World world,
        final TextComponentString message,
        final boolean actionBar
    ) {
        if (player == null || message == null) {
            return;
        }

        long tick = world == null ? 0L : world.getTotalWorldTime();
        String key = player.getName() + "|" + actionBar + "|" + message.getUnformattedText();
        long now = System.currentTimeMillis();
        synchronized (RECENT_MESSAGES) {
            Long lastTick = RECENT_MESSAGES.get(key + "|tick");
            if (lastTick != null && tick - lastTick <= 1L) {
                return;
            }
            Long lastTime = RECENT_MESSAGES.get(key + "|time");
            if (lastTime != null && now - lastTime <= MESSAGE_DEDUP_WINDOW_MS) {
                return;
            }
            RECENT_MESSAGES.put(key + "|tick", tick);
            RECENT_MESSAGES.put(key + "|time", now);
        }

        PlayerCompat.sendStatusMessage(player, message, actionBar);
    }

    private void openTerminalForServerPlayer(final EntityPlayer player, final String networkId) {
        if (player instanceof EntityPlayerMP && !isNullOrEmpty(networkId)) {
            NetworkHandler.CHANNEL.sendTo(new MessageOpenNetworkTerminal(networkId), (EntityPlayerMP) player);
        }
    }

    private void applyTranslationKey(final String key) {
        invokeItemStringMethod("setTranslationKey", key);
        invokeItemStringMethod("setUnlocalizedName", key);

        String shortKey = key;
        int separator = key.lastIndexOf('.');
        if (separator >= 0 && separator + 1 < key.length()) {
            shortKey = key.substring(separator + 1);
        }
        invokeItemStringMethod("setUnlocalizedName", shortKey);
    }

    private boolean invokeItemStringMethod(final String methodName, final String value) {
        try {
            Method method = Item.class.getMethod(methodName, String.class);
            method.invoke(this, value);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    private void applyCreativeTab() {
        CreativeTabs tab = getCreativeTab("MISC");
        if (tab == null) {
            tab = getCreativeTab("SEARCH");
        }
        if (tab != null) {
            setCreativeTab(tab);
        }
    }

    @Nullable
    private CreativeTabs getCreativeTab(final String fieldName) {
        try {
            Field field = CreativeTabs.class.getField(fieldName);
            Object value = field.get(null);
            return value instanceof CreativeTabs ? (CreativeTabs) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }

    private void applyMaxStackSize(final int size) {
        try {
            Method method = Item.class.getMethod("setMaxStackSize", int.class);
            method.invoke(this, size);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Field field = Item.class.getDeclaredField("maxStackSize");
            field.setAccessible(true);
            field.setInt(this, size);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    private enum LinkerMode {
        BIND("绑定"),
        COPY("复制"),
        UNBIND("解绑"),
        INSPECT("查看");

        private final String displayName;

        LinkerMode(final String displayName) {
            this.displayName = displayName;
        }
    }
}

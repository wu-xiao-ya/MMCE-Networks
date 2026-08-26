package com.mmce.networks.common.item;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.data.NetworkAccess;
import com.mmce.networks.common.mmce.MmceReflection;
import com.mmce.networks.common.tile.TileComputeEndpoint;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

public class ItemComputeBinder extends Item {
    private static final String NETWORK_ID_TAG = "networkId";
    private static final String CONTROLLER_DIMENSION_TAG = "controllerDimension";
    private static final String CONTROLLER_POSITION_TAG = "controllerPosition";

    private final MmceReflection reflection = new MmceReflection();

    public ItemComputeBinder() {
        setRegistryName(new ResourceLocation(MMCENetworksMod.MOD_ID, "compute_binder"));
        setTranslationKey(MMCENetworksMod.MOD_ID + ".compute_binder");
        setCreativeTab(CreativeTabs.REDSTONE);
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUseFirst(
        final EntityPlayer player,
        final World world,
        final BlockPos pos,
        final EnumFacing side,
        final float hitX,
        final float hitY,
        final float hitZ,
        final EnumHand hand
    ) {
        if (hand != EnumHand.MAIN_HAND) {
            return EnumActionResult.PASS;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileComputeEndpoint) && !reflection.isControllerTile(tile)) {
            return EnumActionResult.PASS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        ItemStack stack = PlayerCompat.getHeldItem(player, hand);
        if (stack == null) {
            return EnumActionResult.FAIL;
        }
        if (tile instanceof TileComputeEndpoint) {
            TileComputeEndpoint endpoint = (TileComputeEndpoint) tile;
            if (PlayerCompat.isSneaking(player)) {
                if (endpoint.isAutomaticBinding()) {
                    send(player, TextFormatting.GOLD + "结构内算力端点由当前 MMCE 控制器自动管理。");
                    return EnumActionResult.SUCCESS;
                }
                endpoint.clearBinding();
                send(player, TextFormatting.GOLD + "已解除算力端点绑定。");
                return EnumActionResult.SUCCESS;
            }
            if (endpoint.isAutomaticBinding()) {
                send(
                    player,
                    TextFormatting.GREEN + "该端点已由 MMCE 结构自动绑定到控制器 "
                        + TextFormatting.AQUA + endpoint.getControllerPos()
                );
                return EnumActionResult.SUCCESS;
            }
            return bindEndpoint(player, world, stack, endpoint);
        }
        return selectController(player, world, stack, tile, pos);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(
        final World world,
        final EntityPlayer player,
        final EnumHand hand
    ) {
        ItemStack stack = PlayerCompat.getHeldItem(player, hand);
        if (stack == null) {
            stack = new ItemStack(this);
        }
        if (!PlayerCompat.isSneaking(player)) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if (!world.isRemote) {
            clearSelection(stack);
            send(player, TextFormatting.GOLD + "已清除绑定器中的控制器记录。");
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private EnumActionResult selectController(
        final EntityPlayer player,
        final World world,
        final ItemStack stack,
        final TileEntity controller,
        final BlockPos controllerPos
    ) {
        String networkId = reflection.getBoundNetworkId(controller);
        if (networkId == null || networkId.trim().isEmpty()) {
            send(player, TextFormatting.RED + "该控制器尚未绑定 MMCE N 网络。");
            return EnumActionResult.SUCCESS;
        }
        MMCENetworkSavedData data = MMCENetworkSavedData.get(world);
        if (player instanceof EntityPlayerMP
            && !NetworkAccess.canAccess(
                (EntityPlayerMP) player,
                data,
                world.provider.getDimension(),
                networkId
            )) {
            send(player, TextFormatting.RED + "你没有权限使用该网络。");
            return EnumActionResult.SUCCESS;
        }

        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setString(NETWORK_ID_TAG, networkId);
        tag.setInteger(CONTROLLER_DIMENSION_TAG, world.provider.getDimension());
        tag.setLong(CONTROLLER_POSITION_TAG, controllerPos.toLong());
        ItemStackCompat.setTagCompound(stack, tag);
        send(
            player,
            TextFormatting.GREEN + "已记录控制器："
                + TextFormatting.AQUA + controllerPos
                + TextFormatting.GREEN + "，网络 "
                + TextFormatting.AQUA + networkId
        );
        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult bindEndpoint(
        final EntityPlayer player,
        final World world,
        final ItemStack stack,
        final TileComputeEndpoint endpoint
    ) {
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        if (tag == null
            || !tag.hasKey(NETWORK_ID_TAG, Constants.NBT.TAG_STRING)
            || !tag.hasKey(CONTROLLER_DIMENSION_TAG, Constants.NBT.TAG_INT)
            || !tag.hasKey(CONTROLLER_POSITION_TAG, Constants.NBT.TAG_LONG)) {
            send(player, TextFormatting.RED + "请先右键一个已绑定网络的 MMCE 控制器。");
            return EnumActionResult.SUCCESS;
        }

        String networkId = tag.getString(NETWORK_ID_TAG).trim();
        int dimension = tag.getInteger(CONTROLLER_DIMENSION_TAG);
        BlockPos controllerPos = BlockPos.fromLong(tag.getLong(CONTROLLER_POSITION_TAG));
        if (dimension != world.provider.getDimension()) {
            send(player, TextFormatting.RED + "算力端点与控制器必须位于同一维度。");
            return EnumActionResult.SUCCESS;
        }
        if (!world.isBlockLoaded(controllerPos)) {
            send(player, TextFormatting.RED + "目标控制器区块未加载，无法确认绑定。");
            return EnumActionResult.SUCCESS;
        }

        TileEntity controller = world.getTileEntity(controllerPos);
        String currentNetworkId = reflection.getBoundNetworkId(controller);
        if (!reflection.isControllerTile(controller) || !networkId.equals(currentNetworkId)) {
            send(player, TextFormatting.RED + "记录的控制器已失效或网络已经改变。");
            return EnumActionResult.SUCCESS;
        }
        MMCENetworkSavedData data = MMCENetworkSavedData.get(world);
        if (player instanceof EntityPlayerMP
            && !NetworkAccess.canAccess((EntityPlayerMP) player, data, dimension, networkId)) {
            send(player, TextFormatting.RED + "你没有权限使用该网络。");
            return EnumActionResult.SUCCESS;
        }

        endpoint.bind(networkId, dimension, controllerPos);
        send(
            player,
            TextFormatting.GREEN + "算力端点已绑定到控制器 "
                + TextFormatting.AQUA + controllerPos
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
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        if (tag == null || !tag.hasKey(NETWORK_ID_TAG, Constants.NBT.TAG_STRING)) {
            tooltip.add(TextFormatting.GRAY + "未记录控制器");
        } else {
            tooltip.add(TextFormatting.AQUA + "网络：" + TextFormatting.WHITE + tag.getString(NETWORK_ID_TAG));
            tooltip.add(
                TextFormatting.AQUA + "控制器："
                    + TextFormatting.WHITE
                    + BlockPos.fromLong(tag.getLong(CONTROLLER_POSITION_TAG))
            );
        }
        tooltip.add(TextFormatting.YELLOW + "用于结构外端点或调试绑定");
        tooltip.add(TextFormatting.GRAY + "结构内端点会自动绑定当前控制器");
    }

    private static NBTTagCompound getOrCreateTag(final ItemStack stack) {
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        return tag == null ? new NBTTagCompound() : tag;
    }

    private static void clearSelection(final ItemStack stack) {
        NBTTagCompound tag = ItemStackCompat.getTagCompound(stack);
        if (tag == null) {
            return;
        }
        tag.removeTag(NETWORK_ID_TAG);
        tag.removeTag(CONTROLLER_DIMENSION_TAG);
        tag.removeTag(CONTROLLER_POSITION_TAG);
        ItemStackCompat.setTagCompound(stack, tag);
    }

    private static void send(final EntityPlayer player, final String message) {
        if (player != null) {
            player.sendMessage(new TextComponentString(message));
        }
    }
}

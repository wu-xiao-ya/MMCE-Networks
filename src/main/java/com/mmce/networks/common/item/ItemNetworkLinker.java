package com.mmce.networks.common.item;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.mmce.MmceReflection;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemNetworkLinker extends Item {
    private static final String TAG_NETWORK_ID = "networkId";

    private final MmceReflection reflection = new MmceReflection();

    public ItemNetworkLinker() {
        setRegistryName(new ResourceLocation(MMCENetworksMod.MOD_ID, "network_linker"));
        setTranslationKey(MMCENetworksMod.MOD_ID + ".network_linker");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.isSneaking()) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        if (!world.isRemote) {
            String networkId = createNetworkId();
            setNetworkId(stack, networkId);
            player.sendStatusMessage(new TextComponentString("已创建网络: " + networkId), false);
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
        if (!reflection.isAvailable()) {
            return EnumActionResult.PASS;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!reflection.isControllerTile(tile)) {
            return EnumActionResult.PASS;
        }

        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            String boundNetworkId = reflection.getBoundNetworkId(tile);
            if (isNullOrEmpty(boundNetworkId)) {
                player.sendStatusMessage(new TextComponentString("这个控制器还没有绑定网络"), false);
                return EnumActionResult.SUCCESS;
            }

            setNetworkId(stack, boundNetworkId);
            player.sendStatusMessage(new TextComponentString("已复制控制器网络: " + boundNetworkId), false);
            return EnumActionResult.SUCCESS;
        }

        String networkId = getNetworkId(stack);
        if (isNullOrEmpty(networkId)) {
            networkId = createNetworkId();
            setNetworkId(stack, networkId);
            player.sendStatusMessage(new TextComponentString("已创建网络: " + networkId), false);
        }

        MMCENetworkSavedData data = MMCENetworkSavedData.get(world);
        NBTTagCompound sharedData = data.getNetworkData(world.provider.getDimension(), networkId);
        reflection.setSharedData(tile, networkId, sharedData);
        reflection.markForUpdateSync(tile);
        data.putControllerSnapshot(world.provider.getDimension(), pos.toLong(), networkId, sharedData);
        player.sendStatusMessage(new TextComponentString("控制器已绑定到网络: " + networkId), false);
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
        tooltip.add(networkId == null ? "未写入网络 ID" : "网络 ID: " + networkId);
        tooltip.add("潜行右键空气: 生成新网络");
        tooltip.add("右键 MMCE 控制器: 绑定到该网络");
        tooltip.add("潜行右键 MMCE 控制器: 复制控制器网络");
    }

    @Nullable
    public static String getNetworkId(final ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_NETWORK_ID)) {
            return null;
        }
        return tag.getString(TAG_NETWORK_ID);
    }

    private static void setNetworkId(final ItemStack stack, final String networkId) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setString(TAG_NETWORK_ID, networkId);
        stack.setTagCompound(tag);
    }

    private static String createNetworkId() {
        return "net_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }
}

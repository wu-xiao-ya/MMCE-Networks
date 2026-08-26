package com.mmce.networks.common.block;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.compute.transport.ComputeCableNetworkService;
import com.mmce.networks.common.compute.transport.ComputeEndpointType;
import com.mmce.networks.common.item.ItemComputeBinder;
import com.mmce.networks.common.tile.TileComputeEndpoint;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockComputeEndpoint extends Block implements ITileEntityProvider {
    private final ComputeEndpointType endpointType;

    public BlockComputeEndpoint(
        final String registryName,
        final ComputeEndpointType endpointType
    ) {
        super(Material.IRON);
        this.endpointType = endpointType;
        setRegistryName(new ResourceLocation(MMCENetworksMod.MOD_ID, registryName));
        setTranslationKey(MMCENetworksMod.MOD_ID + "." + registryName);
        setHardness(3.5F);
        setResistance(12.0F);
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    public ComputeEndpointType getEndpointType() {
        return endpointType;
    }

    @Override
    public boolean hasTileEntity(final IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(final World worldIn, final int meta) {
        return new TileComputeEndpoint();
    }

    @Override
    public boolean onBlockActivated(
        final World worldIn,
        final BlockPos pos,
        final IBlockState state,
        final EntityPlayer playerIn,
        final EnumHand hand,
        final EnumFacing facing,
        final float hitX,
        final float hitY,
        final float hitZ
    ) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() instanceof ItemComputeBinder) {
            return false;
        }
        if (worldIn.isRemote) {
            return true;
        }
        TileEntity tile = worldIn.getTileEntity(pos);
        if (!(tile instanceof TileComputeEndpoint)) {
            return false;
        }
        TileComputeEndpoint endpoint = (TileComputeEndpoint) tile;
        String text = endpoint.isBound()
            ? (endpoint.isAutomaticBinding() ? "端点已自动绑定：网络 " : "端点已手动绑定：网络 ")
                + endpoint.getNetworkId()
                + "，控制器 " + endpoint.getControllerPos()
            : "端点尚未绑定控制器。";
        playerIn.sendMessage(new TextComponentString(text));
        return true;
    }

    @Override
    public void breakBlock(
        final World worldIn,
        final BlockPos pos,
        final IBlockState state
    ) {
        ComputeCableNetworkService.markGraphDirty(worldIn);
        super.breakBlock(worldIn, pos, state);
    }
}

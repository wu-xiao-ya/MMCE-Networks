package com.mmce.networks.common.block;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.compute.transport.ComputeCableNetworkService;
import com.mmce.networks.common.compute.transport.ComputeEndpointAutoBindingService;
import com.mmce.networks.common.compute.transport.ComputeEndpointAutoBindingService.DiagnosticResult;
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
        ComputeEndpointAutoBindingService.synchronizeNow(worldIn);
        TileComputeEndpoint endpoint = (TileComputeEndpoint) tile;
        String text;
        if (endpoint.isBound()) {
            String bindingType = endpoint.isAutomaticBinding() ? "端点已自动归属：" : "端点已手动归属：";
            String networkStatus = endpoint.hasNetwork()
                ? "，网络 " + endpoint.getNetworkId()
                : "，控制器尚未加入 MMCE N 网络";
            text = bindingType + "控制器 " + endpoint.getControllerPos() + networkStatus;
        } else {
            DiagnosticResult diagnostic = ComputeEndpointAutoBindingService.diagnose(worldIn, pos);
            text = "端点自动归属失败：" + describeDiagnostic(diagnostic);
        }
        playerIn.sendMessage(new TextComponentString(text));
        return true;
    }

    private static String describeDiagnostic(final DiagnosticResult diagnostic) {
        switch (diagnostic) {
            case MMCE_API_UNAVAILABLE:
                return "当前 MMCE 版本缺少结构查询接口。";
            case NO_FORMED_CONTROLLER:
                return "当前维度没有已成型且已加载的 MMCE 控制器。";
            case NOT_IN_FORMED_PATTERN:
                return "该端点不在任何已成型 MMCE 结构的实际坐标中。";
            case AMBIGUOUS:
                return "该端点同时属于多个已成型结构，无法确定唯一控制器。";
            case MATCHED:
                return "已匹配结构，但绑定写入未完成。";
            default:
                return "世界状态无效。";
        }
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

package com.mmce.networks.common.block;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.compute.transport.ComputeCableNetworkService;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class BlockComputeCable extends Block {
    public static final PropertyBool DOWN = PropertyBool.create("down");
    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool EAST = PropertyBool.create("east");

    private static final AxisAlignedBB CORE = new AxisAlignedBB(
        0.3125D, 0.3125D, 0.3125D,
        0.6875D, 0.6875D, 0.6875D
    );
    private static final AxisAlignedBB ARM_DOWN = new AxisAlignedBB(
        0.375D, 0.0D, 0.375D,
        0.625D, 0.3125D, 0.625D
    );
    private static final AxisAlignedBB ARM_UP = new AxisAlignedBB(
        0.375D, 0.6875D, 0.375D,
        0.625D, 1.0D, 0.625D
    );
    private static final AxisAlignedBB ARM_NORTH = new AxisAlignedBB(
        0.375D, 0.375D, 0.0D,
        0.625D, 0.625D, 0.3125D
    );
    private static final AxisAlignedBB ARM_SOUTH = new AxisAlignedBB(
        0.375D, 0.375D, 0.6875D,
        0.625D, 0.625D, 1.0D
    );
    private static final AxisAlignedBB ARM_WEST = new AxisAlignedBB(
        0.0D, 0.375D, 0.375D,
        0.3125D, 0.625D, 0.625D
    );
    private static final AxisAlignedBB ARM_EAST = new AxisAlignedBB(
        0.6875D, 0.375D, 0.375D,
        1.0D, 0.625D, 0.625D
    );

    public BlockComputeCable() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(MMCENetworksMod.MOD_ID, "compute_cable"));
        setTranslationKey(MMCENetworksMod.MOD_ID + ".compute_cable");
        setHardness(1.5F);
        setResistance(6.0F);
        setCreativeTab(CreativeTabs.REDSTONE);
        setDefaultState(blockState.getBaseState()
            .withProperty(DOWN, false)
            .withProperty(UP, false)
            .withProperty(NORTH, false)
            .withProperty(SOUTH, false)
            .withProperty(WEST, false)
            .withProperty(EAST, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    public IBlockState getStateFromMeta(final int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(final IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getActualState(
        final IBlockState state,
        final IBlockAccess worldIn,
        final BlockPos pos
    ) {
        return state
            .withProperty(DOWN, canConnect(worldIn, pos.down()))
            .withProperty(UP, canConnect(worldIn, pos.up()))
            .withProperty(NORTH, canConnect(worldIn, pos.north()))
            .withProperty(SOUTH, canConnect(worldIn, pos.south()))
            .withProperty(WEST, canConnect(worldIn, pos.west()))
            .withProperty(EAST, canConnect(worldIn, pos.east()));
    }

    private boolean canConnect(final IBlockAccess world, final BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return block instanceof BlockComputeCable || block instanceof BlockComputeEndpoint;
    }

    @Override
    public AxisAlignedBB getBoundingBox(
        final IBlockState state,
        final IBlockAccess source,
        final BlockPos pos
    ) {
        IBlockState actual = getActualState(state, source, pos);
        AxisAlignedBB result = CORE;
        if (actual.getValue(DOWN)) {
            result = result.union(ARM_DOWN);
        }
        if (actual.getValue(UP)) {
            result = result.union(ARM_UP);
        }
        if (actual.getValue(NORTH)) {
            result = result.union(ARM_NORTH);
        }
        if (actual.getValue(SOUTH)) {
            result = result.union(ARM_SOUTH);
        }
        if (actual.getValue(WEST)) {
            result = result.union(ARM_WEST);
        }
        if (actual.getValue(EAST)) {
            result = result.union(ARM_EAST);
        }
        return result;
    }

    @Override
    public void addCollisionBoxToList(
        final IBlockState state,
        final World worldIn,
        final BlockPos pos,
        final AxisAlignedBB entityBox,
        final List<AxisAlignedBB> collidingBoxes,
        @Nullable final Entity entityIn,
        final boolean isActualState
    ) {
        IBlockState actual = getActualState(state, worldIn, pos);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, CORE);
        if (actual.getValue(DOWN)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_DOWN);
        }
        if (actual.getValue(UP)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_UP);
        }
        if (actual.getValue(NORTH)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_NORTH);
        }
        if (actual.getValue(SOUTH)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_SOUTH);
        }
        if (actual.getValue(WEST)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_WEST);
        }
        if (actual.getValue(EAST)) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, ARM_EAST);
        }
    }

    @Override
    public boolean isOpaqueCube(final IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(final IBlockState state) {
        return false;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public void onBlockAdded(
        final World worldIn,
        final BlockPos pos,
        final IBlockState state
    ) {
        super.onBlockAdded(worldIn, pos, state);
        ComputeCableNetworkService.markGraphDirty(worldIn);
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

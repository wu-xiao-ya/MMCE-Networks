package com.mmce.networks.common.tile;

import com.mmce.networks.common.block.BlockComputeEndpoint;
import com.mmce.networks.common.compute.transport.ComputeCableNetworkService;
import com.mmce.networks.common.compute.transport.ComputeEndpointType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public class TileComputeEndpoint extends TileEntity {
    private static final String NETWORK_ID_TAG = "networkId";
    private static final String CONTROLLER_DIMENSION_TAG = "controllerDimension";
    private static final String CONTROLLER_POSITION_TAG = "controllerPosition";
    private static final String AUTOMATIC_BINDING_TAG = "automaticBinding";

    private String networkId = "";
    private int controllerDimension;
    private BlockPos controllerPos = BlockPos.ORIGIN;
    private boolean automaticBinding;

    public boolean isBound() {
        return !networkId.isEmpty();
    }

    public String getNetworkId() {
        return networkId;
    }

    public int getControllerDimension() {
        return controllerDimension;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public boolean isAutomaticBinding() {
        return automaticBinding;
    }

    @Nullable
    public ComputeEndpointType getEndpointType() {
        if (world == null) {
            return null;
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block instanceof BlockComputeEndpoint
            ? ((BlockComputeEndpoint) block).getEndpointType()
            : null;
    }

    public boolean matchesBinding(
        final String expectedNetworkId,
        final int expectedDimension,
        final BlockPos expectedControllerPos
    ) {
        return isBound()
            && networkId.equals(expectedNetworkId)
            && controllerDimension == expectedDimension
            && controllerPos.equals(expectedControllerPos);
    }

    public void bind(
        final String newNetworkId,
        final int newControllerDimension,
        final BlockPos newControllerPos
    ) {
        bindInternal(newNetworkId, newControllerDimension, newControllerPos, false);
    }

    public void bindAutomatically(
        final String newNetworkId,
        final int newControllerDimension,
        final BlockPos newControllerPos
    ) {
        bindInternal(newNetworkId, newControllerDimension, newControllerPos, true);
    }

    private void bindInternal(
        final String newNetworkId,
        final int newControllerDimension,
        final BlockPos newControllerPos,
        final boolean automatic
    ) {
        if (world != null && !world.isRemote) {
            ComputeCableNetworkService.unregisterEndpoint(this);
        }
        networkId = newNetworkId == null ? "" : newNetworkId.trim();
        controllerDimension = newControllerDimension;
        controllerPos = newControllerPos == null ? BlockPos.ORIGIN : newControllerPos.toImmutable();
        automaticBinding = automatic;
        bindingChanged();
    }

    public void clearBinding() {
        clearBindingInternal();
    }

    public void clearAutomaticBinding() {
        if (automaticBinding) {
            clearBindingInternal();
        }
    }

    private void clearBindingInternal() {
        if (world != null && !world.isRemote) {
            ComputeCableNetworkService.unregisterEndpoint(this);
        }
        networkId = "";
        controllerDimension = 0;
        controllerPos = BlockPos.ORIGIN;
        automaticBinding = false;
        bindingChanged();
    }

    private void bindingChanged() {
        markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            if (!world.isRemote) {
                ComputeCableNetworkService.registerEndpoint(this);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            ComputeCableNetworkService.registerEndpoint(this);
        }
    }

    @Override
    public void validate() {
        super.validate();
        if (world != null && !world.isRemote) {
            ComputeCableNetworkService.registerEndpoint(this);
        }
    }

    @Override
    public void invalidate() {
        if (world != null && !world.isRemote) {
            ComputeCableNetworkService.unregisterEndpoint(this);
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        if (world != null && !world.isRemote) {
            ComputeCableNetworkService.unregisterEndpoint(this);
        }
        super.onChunkUnload();
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setString(NETWORK_ID_TAG, networkId);
        compound.setInteger(CONTROLLER_DIMENSION_TAG, controllerDimension);
        compound.setLong(CONTROLLER_POSITION_TAG, controllerPos.toLong());
        compound.setBoolean(AUTOMATIC_BINDING_TAG, automaticBinding);
        return compound;
    }

    @Override
    public void readFromNBT(final NBTTagCompound compound) {
        super.readFromNBT(compound);
        networkId = compound.hasKey(NETWORK_ID_TAG, Constants.NBT.TAG_STRING)
            ? compound.getString(NETWORK_ID_TAG).trim()
            : "";
        controllerDimension = compound.getInteger(CONTROLLER_DIMENSION_TAG);
        controllerPos = compound.hasKey(CONTROLLER_POSITION_TAG, Constants.NBT.TAG_LONG)
            ? BlockPos.fromLong(compound.getLong(CONTROLLER_POSITION_TAG))
            : BlockPos.ORIGIN;
        automaticBinding = compound.getBoolean(AUTOMATIC_BINDING_TAG);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(
        final NetworkManager net,
        final SPacketUpdateTileEntity packet
    ) {
        readFromNBT(packet.getNbtCompound());
    }
}

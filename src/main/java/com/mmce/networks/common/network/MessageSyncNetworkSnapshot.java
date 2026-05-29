package com.mmce.networks.common.network;

import com.mmce.networks.client.gui.NetworkTerminalClientState;
import com.mmce.networks.client.util.ClientCompat;
import com.mmce.networks.common.data.NetworkValueDisplayRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSyncNetworkSnapshot implements IMessage {
    private String networkId;
    private NBTTagCompound sharedData;
    private NBTTagCompound valueDisplayConfig;

    public MessageSyncNetworkSnapshot() {
    }

    public MessageSyncNetworkSnapshot(final String networkId, final NBTTagCompound sharedData, final NBTTagCompound valueDisplayConfig) {
        this.networkId = networkId == null ? "" : networkId;
        this.sharedData = sharedData == null ? new NBTTagCompound() : sharedData.copy();
        this.valueDisplayConfig = valueDisplayConfig == null ? new NBTTagCompound() : valueDisplayConfig.copy();
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        networkId = ByteBufUtils.readUTF8String(buf);
        sharedData = ByteBufUtils.readTag(buf);
        valueDisplayConfig = ByteBufUtils.readTag(buf);
        if (sharedData == null) {
            sharedData = new NBTTagCompound();
        }
        if (valueDisplayConfig == null) {
            valueDisplayConfig = new NBTTagCompound();
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, networkId);
        ByteBufUtils.writeTag(buf, sharedData);
        ByteBufUtils.writeTag(buf, valueDisplayConfig);
    }

    public static class Handler implements IMessageHandler<MessageSyncNetworkSnapshot, IMessage> {
        @Override
        public IMessage onMessage(final MessageSyncNetworkSnapshot message, final MessageContext ctx) {
            Minecraft minecraft = ClientCompat.getMinecraft();
            if (minecraft != null) {
                ClientCompat.addScheduledTask(
                    minecraft,
                    () -> NetworkTerminalClientState.applySnapshot(
                        message.networkId,
                        message.sharedData,
                        NetworkValueDisplayRegistry.fromNbt(message.valueDisplayConfig)
                    )
                );
            }
            return null;
        }
    }
}

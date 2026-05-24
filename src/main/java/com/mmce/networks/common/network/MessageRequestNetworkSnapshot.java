package com.mmce.networks.common.network;

import com.mmce.networks.api.MMCENetworkApi;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageRequestNetworkSnapshot implements IMessage {
    private String networkId;

    public MessageRequestNetworkSnapshot() {
    }

    public MessageRequestNetworkSnapshot(final String networkId) {
        this.networkId = networkId == null ? "" : networkId;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        networkId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, networkId);
    }

    public static class Handler implements IMessageHandler<MessageRequestNetworkSnapshot, IMessage> {
        @Override
        public IMessage onMessage(final MessageRequestNetworkSnapshot message, final MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> sendSnapshot(player, message.networkId));
            return null;
        }

        private static void sendSnapshot(final EntityPlayerMP player, final String networkId) {
            NBTTagCompound sharedData = networkId == null || networkId.isEmpty()
                ? new NBTTagCompound()
                : MMCENetworkApi.getSharedData(player.getServerWorld(), networkId);
            NetworkHandler.CHANNEL.sendTo(new MessageSyncNetworkSnapshot(networkId, sharedData), player);
        }
    }
}

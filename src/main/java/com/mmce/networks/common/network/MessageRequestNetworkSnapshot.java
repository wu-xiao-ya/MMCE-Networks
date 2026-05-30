package com.mmce.networks.common.network;

import com.mmce.networks.api.MMCENetworkApi;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.data.MMCENetworkSavedData.NetworkRef;
import com.mmce.networks.common.data.NetworkValueDisplayRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

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
            NBTTagCompound networkListData = buildNetworkListData(player);
            NetworkHandler.CHANNEL.sendTo(
                new MessageSyncNetworkSnapshot(networkId, sharedData, NetworkValueDisplayRegistry.toNbt(), networkListData),
                player
            );
        }

        private static NBTTagCompound buildNetworkListData(final EntityPlayerMP player) {
            NBTTagCompound root = new NBTTagCompound();
            if (player == null || player.world == null) {
                return root;
            }

            MMCENetworkSavedData savedData = MMCENetworkSavedData.get(player.world);
            List<NetworkRef> refs = savedData.getNetworkKeys(player.world.provider.getDimension());
            NBTTagList entries = new NBTTagList();
            for (NetworkRef ref : refs) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("id", ref.getNetworkId());
                entry.setString("displayName", ref.getDisplayName());
                entry.setInteger("color", ref.getColor());
                entry.setBoolean("pinned", ref.isPinned());
                entries.appendTag(entry);
            }
            root.setTag("entries", entries);
            return root;
        }
    }
}

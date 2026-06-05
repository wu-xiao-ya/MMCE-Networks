package com.mmce.networks.common.network;

import com.mmce.networks.api.MMCENetworkApi;
import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.data.NetworkAccess;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageUpdateNetworkStyle implements IMessage {
    private String networkId;
    private int color;
    private boolean pinned;

    public MessageUpdateNetworkStyle() {
    }

    public MessageUpdateNetworkStyle(final String networkId, final int color, final boolean pinned) {
        this.networkId = networkId == null ? "" : networkId;
        this.color = color;
        this.pinned = pinned;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        networkId = ByteBufUtils.readUTF8String(buf);
        color = buf.readInt();
        pinned = buf.readBoolean();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, networkId);
        buf.writeInt(color);
        buf.writeBoolean(pinned);
    }

    public static class Handler implements IMessageHandler<MessageUpdateNetworkStyle, IMessage> {
        @Override
        public IMessage onMessage(final MessageUpdateNetworkStyle message, final MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (message.networkId == null || message.networkId.isEmpty()) {
                    return;
                }
                MMCENetworkSavedData savedData = MMCENetworkSavedData.get(player.world);
                if (!NetworkAccess.canAccess(player, savedData, player.world.provider.getDimension(), message.networkId)) {
                    return;
                }
                MMCENetworkApi.registerNetwork(player.world, message.networkId, player.getUniqueID());
                MMCENetworkApi.setNetworkColor(player.world, message.networkId, message.color);
                MMCENetworkApi.setNetworkPinned(player.world, message.networkId, message.pinned);
            });
            return null;
        }
    }
}

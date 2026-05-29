package com.mmce.networks.common.network;

import com.mmce.networks.api.MMCENetworkApi;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageRenameNetwork implements IMessage {
    private String networkId;
    private String displayName;

    public MessageRenameNetwork() {
    }

    public MessageRenameNetwork(final String networkId, final String displayName) {
        this.networkId = networkId == null ? "" : networkId;
        this.displayName = displayName == null ? "" : displayName;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        networkId = ByteBufUtils.readUTF8String(buf);
        displayName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, networkId);
        ByteBufUtils.writeUTF8String(buf, displayName);
    }

    public static class Handler implements IMessageHandler<MessageRenameNetwork, IMessage> {
        @Override
        public IMessage onMessage(final MessageRenameNetwork message, final MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (message.networkId == null || message.networkId.isEmpty()) {
                    return;
                }
                MMCENetworkApi.registerNetwork(player.world, message.networkId);
                MMCENetworkApi.setNetworkDisplayName(player.world, message.networkId, message.displayName);
            });
            return null;
        }
    }
}

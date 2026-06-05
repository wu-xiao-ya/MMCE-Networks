package com.mmce.networks.common.network;

import com.mmce.networks.common.data.MMCENetworkSavedData;
import com.mmce.networks.common.data.NetworkAccess;
import com.mmce.networks.common.item.ItemNetworkLinker;
import com.mmce.networks.common.util.ItemStackCompat;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class MessageRequestHighlightSync implements IMessage {
    @Override
    public void fromBytes(final io.netty.buffer.ByteBuf buf) {
    }

    @Override
    public void toBytes(final io.netty.buffer.ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<MessageRequestHighlightSync, IMessage> {
        @Override
        public IMessage onMessage(final MessageRequestHighlightSync message, final MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> sync(player));
            return null;
        }

        private static void sync(final EntityPlayerMP player) {
            if (player == null || player.world == null) {
                return;
            }

            ItemStack stack = player.getHeldItemMainhand();
            String networkId = null;
            if (!ItemStackCompat.isEmpty(stack) && stack.getItem() instanceof ItemNetworkLinker) {
                networkId = ItemNetworkLinker.getNetworkId(stack);
            }

            if (networkId == null || networkId.isEmpty()) {
                NetworkHandler.CHANNEL.sendTo(new MessageSyncHighlightPositions(new ArrayList<>()), player);
                return;
            }

            List<BlockPos> positions = new ArrayList<>();
            MMCENetworkSavedData data = MMCENetworkSavedData.get(player.world);
            int dimension = player.world.provider.getDimension();
            if (!NetworkAccess.canAccess(player, data, dimension, networkId)) {
                NetworkHandler.CHANNEL.sendTo(new MessageSyncHighlightPositions(positions), player);
                return;
            }
            for (Long pos : data.getControllerPositions(dimension, networkId)) {
                positions.add(BlockPos.fromLong(pos));
            }
            NetworkHandler.CHANNEL.sendTo(new MessageSyncHighlightPositions(positions), player);
        }
    }
}

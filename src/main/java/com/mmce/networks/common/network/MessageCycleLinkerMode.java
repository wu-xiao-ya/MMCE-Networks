package com.mmce.networks.common.network;

import com.mmce.networks.common.init.ModItems;
import com.mmce.networks.common.item.ItemNetworkLinker;
import com.mmce.networks.common.util.ItemStackCompat;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageCycleLinkerMode implements IMessage {
    private int direction;

    public MessageCycleLinkerMode() {
    }

    public MessageCycleLinkerMode(final int direction) {
        this.direction = direction < 0 ? -1 : 1;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        direction = buf.readInt();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeInt(direction);
    }

    public static class Handler implements IMessageHandler<MessageCycleLinkerMode, IMessage> {
        @Override
        public IMessage onMessage(final MessageCycleLinkerMode message, final MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> cycleHeldLinker(player, message.direction));
            return null;
        }

        private static void cycleHeldLinker(final EntityPlayerMP player, final int direction) {
            for (EnumHand hand : EnumHand.values()) {
                ItemStack stack = player.getHeldItem(hand);
                if (!ItemStackCompat.isEmpty(stack) && stack.getItem() == ModItems.NETWORK_LINKER) {
                    ItemNetworkLinker.cycleMode(stack, direction);
                    ItemNetworkLinker.sendModeMessage(player, stack);
                    return;
                }
            }
        }
    }
}

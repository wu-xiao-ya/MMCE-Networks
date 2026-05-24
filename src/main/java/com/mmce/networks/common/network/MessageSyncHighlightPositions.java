package com.mmce.networks.common.network;

import com.mmce.networks.client.handler.NetworkHighlightRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class MessageSyncHighlightPositions implements IMessage {
    private final List<BlockPos> positions = new ArrayList<>();

    public MessageSyncHighlightPositions() {
    }

    public MessageSyncHighlightPositions(final List<BlockPos> positions) {
        if (positions != null) {
            this.positions.addAll(positions);
        }
    }

    @Override
    public void fromBytes(final io.netty.buffer.ByteBuf buf) {
        positions.clear();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            positions.add(BlockPos.fromLong(buf.readLong()));
        }
    }

    @Override
    public void toBytes(final io.netty.buffer.ByteBuf buf) {
        buf.writeInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeLong(pos.toLong());
        }
    }

    public static class Handler implements IMessageHandler<MessageSyncHighlightPositions, IMessage> {
        @Override
        public IMessage onMessage(final MessageSyncHighlightPositions message, final MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> NetworkHighlightRenderer.updateHighlights(message.positions));
            return null;
        }
    }
}

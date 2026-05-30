package com.mmce.networks.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MessageOpenNetworkTerminal implements IMessage {
    private String networkId;

    public MessageOpenNetworkTerminal() {
    }

    public MessageOpenNetworkTerminal(final String networkId) {
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

    public static class Handler implements IMessageHandler<MessageOpenNetworkTerminal, IMessage> {
        @Override
        public IMessage onMessage(final MessageOpenNetworkTerminal message, final MessageContext ctx) {
            ClientMessageDispatcher.openNetworkTerminal(message.networkId);
            return null;
        }
    }

    private static final class ClientMessageDispatcher {
        private static void openNetworkTerminal(final String networkId) {
            try {
                Class<?> dispatcherClass = Class.forName("com.mmce.networks.client.network.ClientNetworkMessageHandlers");
                Method method = dispatcherClass.getMethod("openNetworkTerminal", String.class);
                method.invoke(null, networkId);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
    }
}

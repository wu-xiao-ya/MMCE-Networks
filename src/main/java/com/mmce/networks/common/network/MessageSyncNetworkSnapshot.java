package com.mmce.networks.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MessageSyncNetworkSnapshot implements IMessage {
    private String networkId;
    private NBTTagCompound sharedData;
    private NBTTagCompound valueDisplayConfig;
    private NBTTagCompound networkListData;

    public MessageSyncNetworkSnapshot() {
    }

    public MessageSyncNetworkSnapshot(
        final String networkId,
        final NBTTagCompound sharedData,
        final NBTTagCompound valueDisplayConfig,
        final NBTTagCompound networkListData
    ) {
        this.networkId = networkId == null ? "" : networkId;
        this.sharedData = sharedData == null ? new NBTTagCompound() : sharedData.copy();
        this.valueDisplayConfig = valueDisplayConfig == null ? new NBTTagCompound() : valueDisplayConfig.copy();
        this.networkListData = networkListData == null ? new NBTTagCompound() : networkListData.copy();
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        networkId = ByteBufUtils.readUTF8String(buf);
        sharedData = ByteBufUtils.readTag(buf);
        valueDisplayConfig = ByteBufUtils.readTag(buf);
        networkListData = ByteBufUtils.readTag(buf);
        if (sharedData == null) {
            sharedData = new NBTTagCompound();
        }
        if (valueDisplayConfig == null) {
            valueDisplayConfig = new NBTTagCompound();
        }
        if (networkListData == null) {
            networkListData = new NBTTagCompound();
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, networkId);
        ByteBufUtils.writeTag(buf, sharedData);
        ByteBufUtils.writeTag(buf, valueDisplayConfig);
        ByteBufUtils.writeTag(buf, networkListData);
    }

    public static class Handler implements IMessageHandler<MessageSyncNetworkSnapshot, IMessage> {
        @Override
        public IMessage onMessage(final MessageSyncNetworkSnapshot message, final MessageContext ctx) {
            ClientMessageDispatcher.applyNetworkSnapshot(
                message.networkId,
                message.sharedData,
                message.valueDisplayConfig,
                message.networkListData
            );
            return null;
        }
    }

    private static final class ClientMessageDispatcher {
        private static void applyNetworkSnapshot(
            final String networkId,
            final NBTTagCompound sharedData,
            final NBTTagCompound valueDisplayConfig,
            final NBTTagCompound networkListData
        ) {
            try {
                Class<?> dispatcherClass = Class.forName("com.mmce.networks.client.network.ClientNetworkMessageHandlers");
                Method method = dispatcherClass.getMethod(
                    "applyNetworkSnapshot",
                    String.class,
                    NBTTagCompound.class,
                    NBTTagCompound.class,
                    NBTTagCompound.class
                );
                method.invoke(null, networkId, sharedData, valueDisplayConfig, networkListData);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
    }
}

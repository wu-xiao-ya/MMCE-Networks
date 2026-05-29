package com.mmce.networks.common.network;

import com.mmce.networks.MMCENetworksMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(MMCENetworksMod.MOD_ID);

    private NetworkHandler() {
    }

    public static void init() {
        CHANNEL.registerMessage(MessageCycleLinkerMode.Handler.class, MessageCycleLinkerMode.class, 0, Side.SERVER);
        CHANNEL.registerMessage(MessageSyncNetworkSnapshot.Handler.class, MessageSyncNetworkSnapshot.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(MessageRequestNetworkSnapshot.Handler.class, MessageRequestNetworkSnapshot.class, 2, Side.SERVER);
        CHANNEL.registerMessage(MessageSyncHighlightPositions.Handler.class, MessageSyncHighlightPositions.class, 3, Side.CLIENT);
        CHANNEL.registerMessage(MessageRequestHighlightSync.Handler.class, MessageRequestHighlightSync.class, 4, Side.SERVER);
        CHANNEL.registerMessage(MessageRenameNetwork.Handler.class, MessageRenameNetwork.class, 5, Side.SERVER);
    }
}

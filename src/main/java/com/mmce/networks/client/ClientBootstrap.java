package com.mmce.networks.client;

import com.mmce.networks.client.handler.LinkerModeInputHandler;
import com.mmce.networks.client.handler.NetworkHighlightRenderer;
import com.mmce.networks.client.handler.NetworkTerminalOpenHandler;
import net.minecraftforge.common.MinecraftForge;

public final class ClientBootstrap {
    private static boolean initialized;

    private ClientBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        MinecraftForge.EVENT_BUS.register(new LinkerModeInputHandler());
        MinecraftForge.EVENT_BUS.register(new NetworkHighlightRenderer());
        MinecraftForge.EVENT_BUS.register(new NetworkTerminalOpenHandler());
    }
}

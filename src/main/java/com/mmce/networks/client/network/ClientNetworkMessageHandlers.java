package com.mmce.networks.client.network;

import com.mmce.networks.client.gui.GuiNetworkTerminal;
import com.mmce.networks.client.gui.NetworkTerminalClientState;
import com.mmce.networks.client.handler.NetworkHighlightRenderer;
import com.mmce.networks.common.data.NetworkValueDisplayRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class ClientNetworkMessageHandlers {
    private ClientNetworkMessageHandlers() {
    }

    public static void applyNetworkSnapshot(
        final String networkId,
        final NBTTagCompound sharedData,
        final NBTTagCompound valueDisplayConfig,
        final NBTTagCompound networkListData
    ) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }

        minecraft.addScheduledTask(
            () -> NetworkTerminalClientState.applySnapshot(
                networkId,
                sharedData,
                NetworkValueDisplayRegistry.fromNbt(valueDisplayConfig),
                NetworkValueDisplayRegistry.layoutFromNbt(valueDisplayConfig),
                NetworkTerminalClientState.readNetworkSummaries(networkListData)
            )
        );
    }

    public static void openNetworkTerminal(final String networkId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || networkId == null || networkId.isEmpty()) {
            return;
        }

        minecraft.addScheduledTask(() -> {
            NetworkTerminalClientState.open(networkId);
            GuiNetworkTerminal.open(networkId);
        });
    }

    public static void updateHighlights(final List<BlockPos> positions) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }

        minecraft.addScheduledTask(() -> NetworkHighlightRenderer.updateHighlights(positions));
    }
}

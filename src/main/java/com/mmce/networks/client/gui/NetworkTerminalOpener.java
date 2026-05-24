package com.mmce.networks.client.gui;

import com.mmce.networks.common.item.ItemNetworkLinker;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public final class NetworkTerminalOpener {
    private NetworkTerminalOpener() {
    }

    public static boolean openForStack(@Nullable final ItemStack stack) {
        if (stack == null) {
            return false;
        }

        String networkId = ItemNetworkLinker.getNetworkId(stack);
        if (networkId == null || networkId.isEmpty()) {
            return false;
        }

        NetworkTerminalClientState.open(networkId);
        GuiNetworkTerminal.open(networkId);
        return true;
    }
}

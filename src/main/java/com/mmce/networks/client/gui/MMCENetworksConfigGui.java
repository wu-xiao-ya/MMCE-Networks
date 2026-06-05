package com.mmce.networks.client.gui;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.config.MMCENetworksConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;

public class MMCENetworksConfigGui extends GuiConfig {
    public MMCENetworksConfigGui(final GuiScreen parentScreen) {
        super(
            parentScreen,
            new ConfigElement(MMCENetworksConfig.getConfiguration().getCategory("client")).getChildElements(),
            MMCENetworksMod.MOD_ID,
            false,
            false,
            MMCENetworksMod.MOD_NAME
        );
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        MMCENetworksConfig.saveAndReload();
    }
}

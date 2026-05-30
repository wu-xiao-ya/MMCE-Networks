package com.mmce.networks.client.handler;

import com.mmce.networks.common.init.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ClientModelRegistryHandler {
    @SubscribeEvent
    public void registerModels(final ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            ModItems.NETWORK_LINKER,
            0,
            new ModelResourceLocation(ModItems.NETWORK_LINKER.getRegistryName(), "inventory")
        );
    }
}

package com.mmce.networks.common.handler;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.init.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = MMCENetworksMod.MOD_ID)
public final class ModRegistryHandler {
    private ModRegistryHandler() {
    }

    @SubscribeEvent
    public static void registerItems(final RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ModItems.NETWORK_LINKER);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(final ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            ModItems.NETWORK_LINKER,
            0,
            new ModelResourceLocation(ModItems.NETWORK_LINKER.getRegistryName(), "inventory")
        );
    }
}

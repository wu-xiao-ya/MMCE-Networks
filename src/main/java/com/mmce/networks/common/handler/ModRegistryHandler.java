package com.mmce.networks.common.handler;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.init.ModItems;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MMCENetworksMod.MOD_ID)
public final class ModRegistryHandler {
    private ModRegistryHandler() {
    }

    @SubscribeEvent
    public static void registerItems(final RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ModItems.NETWORK_LINKER);
    }
}

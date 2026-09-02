package com.mmce.networks.common.handler;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.init.ModBlocks;
import com.mmce.networks.common.init.ModItems;
import com.mmce.networks.common.block.BlockComputeEndpoint;
import com.mmce.networks.common.item.ItemComputeCable;
import com.mmce.networks.common.item.ItemComputeEndpoint;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MMCENetworksMod.MOD_ID)
public final class ModRegistryHandler {
    private ModRegistryHandler() {
    }

    @SubscribeEvent
    public static void registerBlocks(final RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(ModBlocks.ALL);
    }

    @SubscribeEvent
    public static void registerItems(final RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ModItems.NETWORK_LINKER, ModItems.COMPUTE_BINDER);
        for (Block block : ModBlocks.ALL) {
            event.getRegistry().register(
                block instanceof BlockComputeEndpoint
                    ? new ItemComputeEndpoint((BlockComputeEndpoint) block)
                        .setRegistryName(block.getRegistryName())
                    : block == ModBlocks.COMPUTE_CABLE
                        ? new ItemComputeCable(block).setRegistryName(block.getRegistryName())
                    : new ItemBlock(block).setRegistryName(block.getRegistryName())
            );
        }
    }
}

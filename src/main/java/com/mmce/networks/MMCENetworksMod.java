package com.mmce.networks;

import com.mmce.networks.common.handler.ControllerNetworkSyncHandler;
import com.mmce.networks.common.config.MMCENetworksConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = MMCENetworksMod.MOD_ID,
    name = MMCENetworksMod.MOD_NAME,
    version = MMCENetworksMod.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "after:modularmachinery"
)
public class MMCENetworksMod {
    public static final String MOD_ID = "mmcenetworks";
    public static final String MOD_NAME = "MMCE Networks";
    public static final String VERSION = "0.1.0";

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        MMCENetworksConfig.load(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(new ControllerNetworkSyncHandler());
    }
}

package com.mmce.networks;

import com.mmce.networks.common.handler.ControllerNetworkSyncHandler;
import com.mmce.networks.common.config.MMCENetworksConfig;
import com.mmce.networks.common.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mod(
    modid = MMCENetworksMod.MOD_ID,
    name = MMCENetworksMod.MOD_NAME,
    version = MMCENetworksMod.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:modularui;after:modularmachinery"
)
public class MMCENetworksMod {
    public static final String MOD_ID = "mmcenetworks";
    public static final String MOD_NAME = "Modular Machinery: Community Edition Networks";
    public static final String VERSION = "0.1.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        MMCENetworksConfig.load(event.getSuggestedConfigurationFile());
        NetworkHandler.init();
        MinecraftForge.EVENT_BUS.register(new ControllerNetworkSyncHandler());
        if (event.getSide().isClient()) {
            initClient();
        }
    }

    private void initClient() {
        try {
            Class<?> bootstrapClass = Class.forName("com.mmce.networks.client.ClientBootstrap");
            Method method = bootstrapClass.getMethod("init");
            method.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}

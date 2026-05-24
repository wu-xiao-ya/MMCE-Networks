package com.mmce.networks.client.handler;

import com.mmce.networks.common.init.ModItems;
import com.mmce.networks.common.network.MessageCycleLinkerMode;
import com.mmce.networks.common.network.NetworkHandler;
import com.mmce.networks.common.util.ItemStackCompat;
import com.mmce.networks.client.util.ClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LinkerModeInputHandler {
    @SubscribeEvent
    public void onMouseInput(final MouseEvent event) {
        int wheel = event.getDwheel();
        if (wheel == 0) {
            return;
        }

        Minecraft minecraft = ClientCompat.getMinecraft();
        if (minecraft == null) {
            return;
        }
        EntityPlayer player = ClientCompat.getPlayer(minecraft);
        if (player == null || !player.isSneaking() || !isHoldingLinker(player)) {
            return;
        }

        event.setCanceled(true);
        NetworkHandler.CHANNEL.sendToServer(new MessageCycleLinkerMode(wheel > 0 ? 1 : -1));
    }

    private static boolean isHoldingLinker(final EntityPlayer player) {
        ItemStack mainHand = ClientCompat.getMainHand(player);
        ItemStack offHand = ClientCompat.getOffHand(player);
        return isLinker(mainHand) || isLinker(offHand);
    }

    private static boolean isLinker(final ItemStack stack) {
        return !ItemStackCompat.isEmpty(stack) && stack.getItem() == ModItems.NETWORK_LINKER;
    }
}

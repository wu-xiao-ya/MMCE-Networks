package com.mmce.networks.client.handler;

import com.mmce.networks.client.gui.NetworkTerminalOpener;
import com.mmce.networks.client.util.ClientCompat;
import com.mmce.networks.common.init.ModItems;
import com.mmce.networks.common.util.ItemStackCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NetworkTerminalOpenHandler {
    @SubscribeEvent
    public void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != EnumHand.MAIN_HAND || event.getWorld() == null || !event.getWorld().isRemote) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.isSneaking()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (ItemStackCompat.isEmpty(stack) || stack.getItem() != ModItems.NETWORK_LINKER) {
            return;
        }

        Minecraft minecraft = ClientCompat.getMinecraft();
        RayTraceResult hit = ClientCompat.getObjectMouseOver(minecraft);
        if (hit != null && hit.typeOfHit != RayTraceResult.Type.MISS) {
            return;
        }

        if (NetworkTerminalOpener.openForStack(stack)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }
}

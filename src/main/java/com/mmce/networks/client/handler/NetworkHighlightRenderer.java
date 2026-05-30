package com.mmce.networks.client.handler;

import com.mmce.networks.common.item.ItemNetworkLinker;
import com.mmce.networks.common.network.MessageRequestHighlightSync;
import com.mmce.networks.common.network.NetworkHandler;
import com.mmce.networks.common.util.ItemStackCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class NetworkHighlightRenderer {
    private static final int REQUEST_INTERVAL_TICKS = 10;
    private static final float RED = 0.0F;
    private static final float GREEN = 1.0F;
    private static final float BLUE = 0.0F;
    private static final float ALPHA = 0.9F;
    private static final List<BlockPos> HIGHLIGHT_POSITIONS = new ArrayList<>();

    private String lastRequestedNetworkId = "";
    private long lastRequestTick = -1L;

    public static void updateHighlights(final List<BlockPos> positions) {
        synchronized (HIGHLIGHT_POSITIONS) {
            HIGHLIGHT_POSITIONS.clear();
            if (positions != null) {
                HIGHLIGHT_POSITIONS.addAll(positions);
            }
        }
    }

    public static void clearHighlights() {
        synchronized (HIGHLIGHT_POSITIONS) {
            HIGHLIGHT_POSITIONS.clear();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(final RenderWorldLastEvent event) {
        render(event);
    }

    private void render(final RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        net.minecraft.world.World world = minecraft == null ? null : minecraft.world;
        if (minecraft == null || world == null) {
            return;
        }

        EntityPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        String networkId = getHeldNetworkId(player.getHeldItemMainhand(), player.getHeldItemOffhand());
        if (isNullOrEmpty(networkId)) {
            clearHighlights();
            lastRequestedNetworkId = "";
            return;
        }

        requestSyncIfNeeded(world, networkId);

        List<BlockPos> positions;
        synchronized (HIGHLIGHT_POSITIONS) {
            if (HIGHLIGHT_POSITIONS.isEmpty()) {
                return;
            }
            positions = new ArrayList<>(HIGHLIGHT_POSITIONS);
        }

        double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);

        for (BlockPos pos : positions) {
            AxisAlignedBB box = new AxisAlignedBB(pos).grow(0.002D).offset(-cameraX, -cameraY, -cameraZ);
            RenderGlobal.drawSelectionBoundingBox(box, RED, GREEN, BLUE, ALPHA);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void requestSyncIfNeeded(final net.minecraft.world.World world, final String networkId) {
        long tick = world.getTotalWorldTime();
        if (networkId.equals(lastRequestedNetworkId) && tick - lastRequestTick < REQUEST_INTERVAL_TICKS) {
            return;
        }
        lastRequestedNetworkId = networkId;
        lastRequestTick = tick;
        NetworkHandler.CHANNEL.sendToServer(new MessageRequestHighlightSync());
    }

    @Nullable
    private static String getHeldNetworkId(final ItemStack mainHand, final ItemStack offHand) {
        String networkId = getNetworkId(mainHand);
        return networkId != null ? networkId : getNetworkId(offHand);
    }

    @Nullable
    private static String getNetworkId(final ItemStack stack) {
        if (ItemStackCompat.isEmpty(stack) || !(stack.getItem() instanceof ItemNetworkLinker)) {
            return null;
        }
        return ItemNetworkLinker.getNetworkId(stack);
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }
}

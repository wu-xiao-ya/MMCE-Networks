package com.mmce.networks.client.handler;

import com.mmce.networks.MMCENetworksMod;
import com.mmce.networks.common.item.ItemNetworkLinker;
import com.mmce.networks.common.mmce.MmceReflection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class NetworkHighlightRenderer {
    private static final int CACHE_INTERVAL_TICKS = 10;
    private static final float RED = 0.2F;
    private static final float GREEN = 1.0F;
    private static final float BLUE = 0.2F;
    private static final float ALPHA = 0.9F;

    private final MmceReflection reflection = new MmceReflection();
    private final List<BlockPos> highlightedControllers = new ArrayList<>();
    private String cachedNetworkId = "";
    private long lastCacheTick = -1L;

    @Mod.EventBusSubscriber(modid = MMCENetworksMod.MOD_ID, value = Side.CLIENT)
    public static class EventHooks {
        private static final NetworkHighlightRenderer INSTANCE = new NetworkHighlightRenderer();

        @SubscribeEvent
        public static void onRenderWorldLast(final RenderWorldLastEvent event) {
            INSTANCE.render(event);
        }
    }

    private void render(final RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null || !reflection.isAvailable()) {
            return;
        }

        String networkId = getHeldNetworkId(minecraft.player.getHeldItemMainhand(), minecraft.player.getHeldItemOffhand());
        if (isNullOrEmpty(networkId)) {
            highlightedControllers.clear();
            cachedNetworkId = "";
            return;
        }

        refreshCache(minecraft, networkId);
        if (highlightedControllers.isEmpty()) {
            return;
        }

        double cameraX = minecraft.player.lastTickPosX + (minecraft.player.posX - minecraft.player.lastTickPosX) * event.getPartialTicks();
        double cameraY = minecraft.player.lastTickPosY + (minecraft.player.posY - minecraft.player.lastTickPosY) * event.getPartialTicks();
        double cameraZ = minecraft.player.lastTickPosZ + (minecraft.player.posZ - minecraft.player.lastTickPosZ) * event.getPartialTicks();

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

        for (BlockPos pos : highlightedControllers) {
            AxisAlignedBB box = new AxisAlignedBB(pos).grow(0.002D).offset(-cameraX, -cameraY, -cameraZ);
            RenderGlobal.drawSelectionBoundingBox(box, RED, GREEN, BLUE, ALPHA);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void refreshCache(final Minecraft minecraft, final String networkId) {
        long tick = minecraft.world.getTotalWorldTime();
        if (networkId.equals(cachedNetworkId) && tick - lastCacheTick < CACHE_INTERVAL_TICKS) {
            return;
        }

        highlightedControllers.clear();
        cachedNetworkId = networkId;
        lastCacheTick = tick;

        for (TileEntity tile : minecraft.world.loadedTileEntityList) {
            if (!reflection.isControllerTile(tile)) {
                continue;
            }

            String controllerNetworkId = reflection.getBoundNetworkId(tile);
            if (networkId.equals(controllerNetworkId)) {
                highlightedControllers.add(tile.getPos());
            }
        }
    }

    @Nullable
    private static String getHeldNetworkId(final ItemStack mainHand, final ItemStack offHand) {
        String networkId = getNetworkId(mainHand);
        return networkId != null ? networkId : getNetworkId(offHand);
    }

    @Nullable
    private static String getNetworkId(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemNetworkLinker)) {
            return null;
        }
        return ItemNetworkLinker.getNetworkId(stack);
    }

    private static boolean isNullOrEmpty(@Nullable final String value) {
        return value == null || value.isEmpty();
    }
}

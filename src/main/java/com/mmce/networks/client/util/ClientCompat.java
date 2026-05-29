package com.mmce.networks.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ClientCompat {
    private ClientCompat() {
    }

    @Nullable
    public static Minecraft getMinecraft() {
        try {
            Method method = Minecraft.class.getMethod("getMinecraft");
            Object value = method.invoke(null);
            return value instanceof Minecraft ? (Minecraft) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = Minecraft.class.getMethod("func_71410_x");
            Object value = method.invoke(null);
            return value instanceof Minecraft ? (Minecraft) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        return null;
    }

    public static ItemStack getMainHand(final EntityPlayer player) {
        ItemStack stack = invokePlayerStackGetter(player, "getHeldItemMainhand");
        return stack != null ? stack : invokePlayerStackGetter(player, "func_184614_ca");
    }

    public static ItemStack getOffHand(final EntityPlayer player) {
        ItemStack stack = invokePlayerStackGetter(player, "getHeldItemOffhand");
        return stack != null ? stack : invokePlayerStackGetter(player, "func_184592_cb");
    }

    @Nullable
    public static EntityPlayer getPlayer(final Minecraft minecraft) {
        if (minecraft == null) {
            return null;
        }

        try {
            Field field = Minecraft.class.getField("player");
            Object value = field.get(minecraft);
            return value instanceof EntityPlayer ? (EntityPlayer) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            Field field = Minecraft.class.getField("field_71439_g");
            Object value = field.get(minecraft);
            return value instanceof EntityPlayer ? (EntityPlayer) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        return null;
    }

    @Nullable
    public static World getWorld(final Minecraft minecraft) {
        if (minecraft == null) {
            return null;
        }

        try {
            Field field = Minecraft.class.getField("world");
            Object value = field.get(minecraft);
            return value instanceof World ? (World) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            Field field = Minecraft.class.getField("field_71441_e");
            Object value = field.get(minecraft);
            return value instanceof World ? (World) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        return null;
    }

    public static void displayGuiScreen(final Minecraft minecraft, final GuiScreen screen) {
        if (minecraft == null) {
            return;
        }

        try {
            minecraft.displayGuiScreen(screen);
            return;
        } catch (NoSuchMethodError ignored) {
        }

        try {
            Method method = Minecraft.class.getMethod("displayGuiScreen", GuiScreen.class);
            method.invoke(minecraft, screen);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = Minecraft.class.getMethod("func_147108_a", GuiScreen.class);
            method.invoke(minecraft, screen);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    public static void addScheduledTask(final Minecraft minecraft, final Runnable task) {
        if (minecraft == null || task == null) {
            return;
        }

        try {
            minecraft.addScheduledTask(task);
            return;
        } catch (NoSuchMethodError ignored) {
        }

        try {
            Method method = Minecraft.class.getMethod("addScheduledTask", Runnable.class);
            method.invoke(minecraft, task);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = Minecraft.class.getMethod("func_152344_a", Runnable.class);
            method.invoke(minecraft, task);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    @Nullable
    public static RayTraceResult getObjectMouseOver(final Minecraft minecraft) {
        if (minecraft == null) {
            return null;
        }

        try {
            Field field = Minecraft.class.getField("objectMouseOver");
            Object value = field.get(minecraft);
            return value instanceof RayTraceResult ? (RayTraceResult) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            Field field = Minecraft.class.getField("field_71476_x");
            Object value = field.get(minecraft);
            return value instanceof RayTraceResult ? (RayTraceResult) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        return null;
    }

    @Nullable
    private static ItemStack invokePlayerStackGetter(final EntityPlayer player, final String methodName) {
        if (player == null) {
            return null;
        }
        try {
            Method method = EntityPlayer.class.getMethod(methodName);
            Object value = method.invoke(player);
            return value instanceof ItemStack ? (ItemStack) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }
}

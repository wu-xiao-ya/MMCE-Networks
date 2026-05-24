package com.mmce.networks.common.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PlayerCompat {
    private PlayerCompat() {
    }

    public static ItemStack getHeldItem(final EntityPlayer player, final EnumHand hand) {
        if (player == null || hand == null) {
            return null;
        }

        try {
            return player.getHeldItem(hand);
        } catch (NoSuchMethodError ignored) {
        }

        try {
            Method method = EntityPlayer.class.getMethod("getHeldItem", EnumHand.class);
            Object value = method.invoke(player, hand);
            return value instanceof ItemStack ? (ItemStack) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        return null;
    }

    public static void sendStatusMessage(final EntityPlayer player, final ITextComponent message, final boolean actionBar) {
        if (player == null || message == null) {
            return;
        }

        try {
            player.sendStatusMessage(message, actionBar);
            return;
        } catch (NoSuchMethodError ignored) {
        }

        try {
            Method method = EntityPlayer.class.getMethod("sendStatusMessage", ITextComponent.class, boolean.class);
            method.invoke(player, message, actionBar);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = EntityPlayer.class.getMethod("func_146105_b", ITextComponent.class, boolean.class);
            method.invoke(player, message, actionBar);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}

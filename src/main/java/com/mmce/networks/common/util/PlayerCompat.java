package com.mmce.networks.common.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PlayerCompat {
    private static final Method GET_HELD_ITEM_METHOD = findMethod("getHeldItem", EnumHand.class);
    private static final Method GET_HELD_ITEM_SRG_METHOD = findMethod("func_184586_b", EnumHand.class);
    private static final Method GET_MAIN_HAND_METHOD = findMethod("getHeldItemMainhand");
    private static final Method GET_MAIN_HAND_SRG_METHOD = findMethod("func_184614_ca");
    private static final Method GET_OFF_HAND_METHOD = findMethod("getHeldItemOffhand");
    private static final Method GET_OFF_HAND_SRG_METHOD = findMethod("func_184592_cb");
    private static final Method SEND_STATUS_MESSAGE_METHOD = findMethod("sendStatusMessage", ITextComponent.class, boolean.class);
    private static final Method SEND_STATUS_MESSAGE_SRG_METHOD = findMethod("func_146105_b", ITextComponent.class, boolean.class);
    private static final Method IS_SNEAKING_METHOD = findMethod("isSneaking");
    private static final Method IS_SNEAKING_SRG_METHOD = findMethod("func_70093_af");

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

        ItemStack reflected = invokeItemStack(player, GET_HELD_ITEM_METHOD, hand);
        if (reflected != null) {
            return reflected;
        }

        reflected = invokeItemStack(player, GET_HELD_ITEM_SRG_METHOD, hand);
        if (reflected != null) {
            return reflected;
        }

        if (hand == EnumHand.MAIN_HAND) {
            reflected = invokeItemStack(player, GET_MAIN_HAND_METHOD);
            if (reflected != null) {
                return reflected;
            }
            reflected = invokeItemStack(player, GET_MAIN_HAND_SRG_METHOD);
            if (reflected != null) {
                return reflected;
            }
            reflected = getCurrentInventoryStack(player);
            if (reflected != null) {
                return reflected;
            }
        }

        if (hand == EnumHand.OFF_HAND) {
            reflected = invokeItemStack(player, GET_OFF_HAND_METHOD);
            if (reflected != null) {
                return reflected;
            }
            reflected = invokeItemStack(player, GET_OFF_HAND_SRG_METHOD);
            if (reflected != null) {
                return reflected;
            }
        }

        return null;
    }

    public static boolean isSneaking(final EntityPlayer player) {
        if (player == null) {
            return false;
        }

        try {
            return player.isSneaking();
        } catch (NoSuchMethodError ignored) {
        }

        Boolean reflected = invokeBoolean(player, IS_SNEAKING_METHOD);
        if (reflected != null) {
            return reflected;
        }

        reflected = invokeBoolean(player, IS_SNEAKING_SRG_METHOD);
        return reflected != null && reflected;
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

        if (invokeVoid(player, SEND_STATUS_MESSAGE_METHOD, message, actionBar)) {
            return;
        }

        invokeVoid(player, SEND_STATUS_MESSAGE_SRG_METHOD, message, actionBar);
    }

    private static Method findMethod(final String name, final Class<?>... parameterTypes) {
        try {
            return EntityPlayer.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Boolean invokeBoolean(final EntityPlayer player, final Method method, final Object... args) {
        if (player == null || method == null) {
            return null;
        }

        try {
            Object value = method.invoke(player, args);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static ItemStack invokeItemStack(final EntityPlayer player, final Method method, final Object... args) {
        if (player == null || method == null) {
            return null;
        }

        try {
            Object value = method.invoke(player, args);
            return value instanceof ItemStack ? (ItemStack) value : null;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static ItemStack getCurrentInventoryStack(final EntityPlayer player) {
        if (player == null || player.inventory == null) {
            return null;
        }

        try {
            ItemStack stack = player.inventory.getCurrentItem();
            if (stack != null) {
                return stack;
            }
        } catch (NoSuchMethodError ignored) {
        }

        try {
            Method method = player.inventory.getClass().getMethod("getCurrentItem");
            Object value = method.invoke(player.inventory);
            return value instanceof ItemStack ? (ItemStack) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = player.inventory.getClass().getMethod("func_70448_g");
            Object value = method.invoke(player.inventory);
            return value instanceof ItemStack ? (ItemStack) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Field currentItemField = player.inventory.getClass().getField("currentItem");
            int currentItem = currentItemField.getInt(player.inventory);
            if (currentItem >= 0 && currentItem < player.inventory.mainInventory.size()) {
                return player.inventory.mainInventory.get(currentItem);
            }
        } catch (NoSuchFieldException | IllegalAccessException | IndexOutOfBoundsException ignored) {
        }

        return null;
    }

    private static boolean invokeVoid(final EntityPlayer player, final Method method, final Object... args) {
        if (player == null || method == null) {
            return false;
        }

        try {
            method.invoke(player, args);
            return true;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }
}

package com.mmce.networks.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ItemStackCompat {
    private ItemStackCompat() {
    }

    public static boolean isEmpty(final ItemStack stack) {
        if (stack == null) {
            return true;
        }

        try {
            return stack.isEmpty();
        } catch (NoSuchMethodError ignored) {
        }

        try {
            Method method = ItemStack.class.getMethod("isEmpty");
            Object value = method.invoke(stack);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = ItemStack.class.getMethod("func_190926_b");
            Object value = method.invoke(stack);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        if (stack.getItem() == null) {
            return true;
        }

        Integer size = getStackSize(stack);
        return size != null && size <= 0;
    }

    private static Integer getStackSize(final ItemStack stack) {
        try {
            Field field = ItemStack.class.getDeclaredField("stackSize");
            field.setAccessible(true);
            return field.getInt(stack);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            Field field = ItemStack.class.getDeclaredField("field_77994_a");
            field.setAccessible(true);
            return field.getInt(stack);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        return null;
    }

    public static NBTTagCompound getTagCompound(final ItemStack stack) {
        if (stack == null) {
            return null;
        }

        try {
            return stack.getTagCompound();
        } catch (NoSuchMethodError ignored) {
        }

        try {
            Method method = ItemStack.class.getMethod("getTagCompound");
            Object value = method.invoke(stack);
            return value instanceof NBTTagCompound ? (NBTTagCompound) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = ItemStack.class.getMethod("func_77978_p");
            Object value = method.invoke(stack);
            return value instanceof NBTTagCompound ? (NBTTagCompound) value : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        return null;
    }

    public static void setTagCompound(final ItemStack stack, final NBTTagCompound tag) {
        if (stack == null) {
            return;
        }

        try {
            stack.setTagCompound(tag);
            return;
        } catch (NoSuchMethodError ignored) {
        }

        try {
            Method method = ItemStack.class.getMethod("setTagCompound", NBTTagCompound.class);
            method.invoke(stack, tag);
            return;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            Method method = ItemStack.class.getMethod("func_77982_d", NBTTagCompound.class);
            method.invoke(stack, tag);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}

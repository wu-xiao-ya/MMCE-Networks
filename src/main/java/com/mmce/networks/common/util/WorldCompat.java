package com.mmce.networks.common.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.MapStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.lang.reflect.Field;
import java.util.List;
import java.lang.reflect.Method;

public final class WorldCompat {
    private WorldCompat() {
    }

    public static boolean isRemote(final World world) {
        if (world == null) {
            return false;
        }

        try {
            Field field = World.class.getField("isRemote");
            return field.getBoolean(world);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            Method method = World.class.getMethod("isRemote");
            Object value = method.invoke(world);
            return value instanceof Boolean && (Boolean) value;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method method = World.class.getMethod("func_72912_H");
            return method.invoke(world) == null;
        } catch (ReflectiveOperationException ignored) {
        }

        return false;
    }

    public static int getDimension(final World world) {
        if (world == null) {
            return 0;
        }

        try {
            Field field = World.class.getField("provider");
            Object value = field.get(world);
            if (value instanceof WorldProvider) {
                return ((WorldProvider) value).getDimension();
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            Method method = World.class.getMethod("provider");
            Object value = method.invoke(world);
            if (value instanceof WorldProvider) {
                return ((WorldProvider) value).getDimension();
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method method = World.class.getMethod("getDimension");
            Object value = method.invoke(world);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return 0;
    }

    public static long getTotalWorldTime(final World world) {
        if (world == null) {
            return 0L;
        }

        try {
            Method method = World.class.getMethod("getTotalWorldTime");
            Object value = method.invoke(world);
            if (value instanceof Long) {
                return (Long) value;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method method = World.class.getMethod("func_82737_E");
            Object value = method.invoke(world);
            if (value instanceof Long) {
                return (Long) value;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return 0L;
    }

    public static List<TileEntity> getLoadedTileEntities(final World world) {
        if (world == null) {
            return Collections.emptyList();
        }

        try {
            Field field = World.class.getField("loadedTileEntityList");
            Object value = field.get(world);
            if (value instanceof List) {
                return new ArrayList<>((List<TileEntity>) value);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            Method method = World.class.getMethod("getLoadedTileEntityList");
            Object value = method.invoke(world);
            if (value instanceof List) {
                return new ArrayList<>((List<TileEntity>) value);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method method = World.class.getMethod("func_147486_a");
            Object value = method.invoke(world);
            if (value instanceof List) {
                return new ArrayList<>((List<TileEntity>) value);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return Collections.emptyList();
    }

    public static MapStorage getPerWorldStorage(final World world) {
        if (world == null) {
            return null;
        }

        try {
            Method method = World.class.getMethod("getPerWorldStorage");
            Object value = method.invoke(world);
            return value instanceof MapStorage ? (MapStorage) value : null;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method method = World.class.getMethod("func_72943_a", String.class);
            method.invoke(world, "");
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Field field = World.class.getField("perWorldStorage");
            Object value = field.get(world);
            return value instanceof MapStorage ? (MapStorage) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            Field field = World.class.getField("field_72988_C");
            Object value = field.get(world);
            return value instanceof MapStorage ? (MapStorage) value : null;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        return null;
    }

    public static TileEntity getTileEntity(final World world, final BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }

        try {
            Method method = World.class.getMethod("getTileEntity", BlockPos.class);
            Object value = method.invoke(world, pos);
            return value instanceof TileEntity ? (TileEntity) value : null;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method method = World.class.getMethod("func_175625_s", BlockPos.class);
            Object value = method.invoke(world, pos);
            return value instanceof TileEntity ? (TileEntity) value : null;
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }
}

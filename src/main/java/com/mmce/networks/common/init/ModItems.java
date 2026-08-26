package com.mmce.networks.common.init;

import com.mmce.networks.common.item.ItemComputeBinder;
import com.mmce.networks.common.item.ItemNetworkLinker;
import net.minecraft.item.Item;

public final class ModItems {
    public static final Item NETWORK_LINKER = new ItemNetworkLinker();
    public static final Item COMPUTE_BINDER = new ItemComputeBinder();

    private ModItems() {
    }
}

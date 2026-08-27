package com.mmce.networks.common.item;

import com.mmce.networks.common.block.BlockComputeEndpoint;
import com.mmce.networks.common.compute.transport.ComputeEndpointType;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemComputeEndpoint extends ItemBlock {
    private final ComputeEndpointType endpointType;

    public ItemComputeEndpoint(final BlockComputeEndpoint block) {
        super(block);
        this.endpointType = block.getEndpointType();
    }

    @Override
    public void addInformation(
        final ItemStack stack,
        @Nullable final World world,
        final List<String> tooltip,
        final ITooltipFlag flag
    ) {
        super.addInformation(stack, world, tooltip, flag);
        if (endpointType == null) {
            return;
        }
        tooltip.add(endpointType.getDescription());
        if (endpointType == ComputeEndpointType.MACHINE_PORT) {
            tooltip.add("\u8fde\u63a5\u65b9\u5f0f\uff1a\u673a\u5668\u5185\u90e8\u81ea\u52a8\u5f52\u5c5e");
        } else if (endpointType == ComputeEndpointType.WIRED_INTERFACE) {
            tooltip.add("\u8fde\u63a5\u65b9\u5f0f\uff1a\u673a\u5668\u7aef\u53e3 \u2192 \u7ebf\u7f06 \u2192 \u6b64\u7aef");
        } else if (endpointType == ComputeEndpointType.WIRELESS_INTERFACE) {
            tooltip.add("\u8fde\u63a5\u65b9\u5f0f\uff1a\u673a\u5668\u5728\u8986\u76d6\u8303\u56f4\u5185\uff0c\u6b64\u7aef\u7528\u7ebf\u7f06\u4e0a\u8054");
        } else if (endpointType == ComputeEndpointType.DISTRIBUTOR) {
            tooltip.add("\u8fde\u63a5\u65b9\u5f0f\uff1a\u4e0a\u8054\u77e9\u9635\uff0c\u4e0b\u8054\u63a5\u5165\u7aef");
        } else if (endpointType == ComputeEndpointType.MATRIX) {
            tooltip.add("\u8fde\u63a5\u65b9\u5f0f\uff1a\u8fde\u63a5\u7f51\u7edc\u6839\u8282\u70b9");
        }
    }
}

package com.mmce.networks.common.item;

import com.mmce.networks.common.block.BlockComputeEndpoint;
import com.mmce.networks.common.compute.transport.ComputeEndpointType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemComputeEndpoint extends ItemBlock {
    private final ComputeEndpointType endpointType;
    private final long computeThroughput;
    private final String throughputTier;

    public ItemComputeEndpoint(final BlockComputeEndpoint block) {
        super(block);
        this.endpointType = block.getEndpointType();
        this.computeThroughput = block.getComputeThroughput();
        this.throughputTier = block.getThroughputTier();
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

        tooltip.add("\u7c7b\u522b\uff1a" + endpointType.getCategory());
        tooltip.add("\u804c\u8d23\uff1a" + endpointType.getDisplayName());
        tooltip.add(endpointType.getDescription());
        switch (endpointType) {
            case MACHINE_PORT:
                tooltip.add("\u653e\u7f6e\uff1a\u4f9b\u7ed9\u673a\u3001\u6d88\u8d39\u673a\u6216\u7814\u7a76\u7ec8\u7aef\u7684 MMCE \u7ed3\u6784\u5185");
                tooltip.add("\u8fde\u63a5\uff1a\u673a\u5668\u7aef\u53e3 \u2192 \u7ebf\u7f06\u6216\u65e0\u7ebf\u8986\u76d6");
                tooltip.add("\u5fc5\u987b\uff1a\u6bcf\u53f0\u4f7f\u7528\u7b97\u529b\u7684\u673a\u5668\u90fd\u9700\u8981");
                tooltip.add("\u7ed1\u5b9a\uff1a\u6210\u578b\u540e\u81ea\u52a8\u5f52\u5c5e\u5f53\u524d MMCE \u63a7\u5236\u5668");
                break;
            case WIRED_INTERFACE:
                tooltip.add("\u653e\u7f6e\uff1a\u7b97\u529b\u77e9\u9635\u7ed3\u6784\u5185");
                tooltip.add("\u8fde\u63a5\uff1a\u7b97\u529b\u7ebf\u7f06 \u2192 \u6b64\u7aef\u53e3");
                tooltip.add("\u5fc5\u987b\uff1a\u9009\u62e9\u6709\u7ebf\u63a5\u5165\u65f6\u9700\u8981");
                tooltip.add("\u7aef\u53e3\u7b49\u7ea7\uff1a" + throughputTier);
                tooltip.add("\u7269\u7406\u4e0a\u9650\uff1a" + computeThroughput + " CU/t");
                tooltip.add("\u4e0d\u662f\uff1a\u77e9\u9635\u6838\u5fc3\u3001\u7b97\u529b\u6765\u6e90\u6216\u5206\u652f\u4ea4\u6362\u8282\u70b9");
                break;
            case WIRELESS_INTERFACE:
                tooltip.add("\u653e\u7f6e\uff1a\u7b97\u529b\u77e9\u9635\u7ed3\u6784\u5185");
                tooltip.add("\u8fde\u63a5\uff1a\u673a\u5668\u5728\u8986\u76d6\u8303\u56f4\u5185 \u2192 \u6b64\u7aef\u53e3");
                tooltip.add("\u5fc5\u987b\uff1a\u6b64\u7aef\u53e3\u4ecd\u9700\u7528\u5b9e\u4f53\u7ebf\u7f06\u4e0a\u8054\u77e9\u9635\u9aa8\u5e72");
                tooltip.add("\u4e0d\u662f\uff1a\u6574\u4e2a\u7f51\u7edc\u90fd\u53d8\u6210\u65e0\u7ebf");
                break;
            case DISTRIBUTOR:
                tooltip.add("\u653e\u7f6e\uff1a\u77e9\u9635\u5185\u6216\u72ec\u7acb\u5206\u652f\u7ed3\u6784\u5185");
                tooltip.add("\u8fde\u63a5\uff1a\u591a\u6761\u5206\u652f\u7ebf\u8def \u2192 \u6b64\u8282\u70b9 \u2192 \u77e9\u9635\u9aa8\u5e72");
                tooltip.add("\u5fc5\u987b\uff1a\u53ea\u6709\u4f60\u60f3\u8981\u5206\u6d41\u6216\u9650\u5236\u5206\u652f\u65f6\u624d\u9700\u8981");
                tooltip.add("\u4e0d\u662f\uff1a\u7b97\u529b\u6765\u6e90\u3001MMCE N \u63a7\u5236\u5668\u6216\u7b2c\u4e8c\u4e2a\u77e9\u9635");
                break;
            case MATRIX:
                tooltip.add("\u653e\u7f6e\uff1a\u7b97\u529b\u77e9\u9635\u7ed3\u6784\u5185");
                tooltip.add("\u8fde\u63a5\uff1a\u77e9\u9635\u63a5\u5165\u548c\u5206\u652f\u7ebf\u8def\u7684\u9aa8\u5e72\u7ec8\u70b9");
                tooltip.add("\u5fc5\u987b\uff1a\u6bcf\u4e2a\u80fd\u63d0\u4f9b\u7b97\u529b\u7f51\u7edc\u7684\u77e9\u9635\u7ed3\u6784\u90fd\u9700\u8981");
                tooltip.add("\u4e0d\u662f\uff1a\u7b97\u529b\u6765\u6e90\u3001\u673a\u5668\u63a5\u53e3\u6216\u5206\u652f\u4ea4\u6362\u8282\u70b9");
                break;
            default:
                break;
        }
    }
}

package com.mmce.networks.common.item;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Gives the physical cable a visible role in the item list.
 */
public class ItemComputeCable extends ItemBlock {
    public ItemComputeCable(final Block block) {
        super(block);
    }

    @Override
    public void addInformation(
        final ItemStack stack,
        @Nullable final World world,
        final List<String> tooltip,
        final ITooltipFlag flag
    ) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add("\u7c7b\u522b\uff1a\u5b9e\u4f53\u7ebf\u8def");
        tooltip.add("\u4f5c\u7528\uff1a\u8fde\u63a5\u5404\u4e2a\u7b97\u529b\u7aef\u70b9\uff0c\u5f62\u6210\u771f\u5b9e\u53ef\u9a8c\u8bc1\u7684\u7ebf\u8def");
        tooltip.add("\u8fde\u63a5\uff1a\u673a\u5668\u7aef\u53e3\u3001\u77e9\u9635\u63a5\u5165\u7aef\u53e3\u3001\u5206\u652f\u4ea4\u6362\u8282\u70b9\u3001\u77e9\u9635\u6838\u5fc3\u7aef\u53e3");
        tooltip.add("\u4e0d\u63d0\u4f9b\uff1aCU/t\u3001\u7f51\u7edc\u6210\u5458\u8eab\u4efd\u6216\u5206\u652f\u9650\u5236");
    }
}

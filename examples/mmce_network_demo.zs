import mods.mmcenetworks.Networks;

import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.RecipeFinishEvent;
import mods.modularmachinery.MMEvents;
import mods.modularmachinery.MachineTickEvent;

// 把这两个名字改成你自己的机器注册名。
val machineA = "machine_a";
val machineB = "machine_b";

val keyA = "A";
val computeKey = "compute";
val techA = "techA";
val techB = "techB";

function requireNetwork(event as RecipeCheckEvent) as bool {
    if (!Networks.hasNetwork(event.controller)) {
        event.setFailed("machine has no network");
        return false;
    }
    return true;
}

function ensureTechTree(controller as github.kasuminova.mmce.common.helper.IMachineController) {
    Networks.defineTech(controller, techA);
    Networks.defineTech(controller, techB);
    Networks.addTechPrerequisite(controller, techB, techA);
}

// 机器 A：
// 输入 1 个圆石，配方完成后把网络变量 A 增加 20。
RecipeBuilder.newBuilder("network_demo_add_a", machineA, 20)
    .addItemInput(<minecraft:cobblestone> * 1)
    .addFinishHandler(function(event as RecipeFinishEvent) {
        ensureTechTree(event.controller);
        val currentA = Networks.getInt(event.controller, keyA, 0);
        Networks.setInt(event.controller, keyA, currentA + 20);
    })
    .build();

// 机器 B：
// 输入 1 个橡木木板，检查网络变量 A 是否至少为 20；
// 如果足够，配方完成后消耗 20 点 A，并输出 1 个铁锭。
RecipeBuilder.newBuilder("network_demo_consume_a", machineB, 20)
    .addItemInput(<minecraft:planks:0> * 1)
    .addItemOutput(<minecraft:iron_ingot> * 1)
    .addCheckHandler(function(event as RecipeCheckEvent) {
        if (!requireNetwork(event)) {
            return;
        }

        val currentA = Networks.getInt(event.controller, keyA, 0);
        if (currentA < 20) {
            event.setFailed("need A >= 20");
        }
    })
    .addFinishHandler(function(event as RecipeFinishEvent) {
        val currentA = Networks.getInt(event.controller, keyA, 0);
        if (currentA >= 20) {
            Networks.setInt(event.controller, keyA, currentA - 20);
        }
    })
    .build();

// 机器 B GUI 显示：
// 每 tick 把当前网络变量 A 写到控制器状态栏里。
MMEvents.onMachinePostTick(machineB, function(event as MachineTickEvent) {
    if (!Networks.hasNetwork(event.controller)) {
        event.controller.overrideStatusInfo("A = no network");
        return;
    }

    val currentA = Networks.getInt(event.controller, keyA, 0);
    event.controller.overrideStatusInfo("A = " ~ currentA);
});

// 网络资源池示例：
// machineA 运行时给网络提供 5 点算力上限。
MMEvents.onMachinePostTick(machineA, function(event as MachineTickEvent) {
    if (!Networks.hasNetwork(event.controller)) {
        return;
    }

    ensureTechTree(event.controller);
    Networks.setSupply(event.controller, computeKey, 5);
});

// machineB 的配方需要持续占用 3 点算力。
// 检查阶段只看当前可用量是否够。
RecipeBuilder.newBuilder("network_demo_use_compute", machineB, 20)
    .addCheckHandler(function(event as RecipeCheckEvent) {
        if (!requireNetwork(event)) {
            return;
        }
        if (!Networks.canUse(event.controller, computeKey, 3)) {
            event.setFailed("need compute >= 3");
        }
    })
    .addFinishHandler(function(event as RecipeFinishEvent) {
        Networks.clearUsage(event.controller, computeKey);
    })
    .build();

MMEvents.onMachinePostTick(machineB, function(event as MachineTickEvent) {
    if (!Networks.hasNetwork(event.controller)) {
        return;
    }

    if (!Networks.trySetUsage(event.controller, computeKey, 3)) {
        event.controller.overrideStatusInfo("compute busy");
        return;
    }

    val capacity = Networks.getCapacity(event.controller, computeKey);
    val used = Networks.getUsed(event.controller, computeKey);
    event.controller.overrideStatusInfo("compute: " ~ used ~ "/" ~ capacity);
});

// 科技树示例：
// machineA 完成配方后解锁 techA，machineB 再能解锁或使用依赖 techA 的内容。
RecipeBuilder.newBuilder("network_demo_unlock_techa", machineA, 20)
    .addItemInput(<minecraft:redstone> * 1)
    .addFinishHandler(function(event as RecipeFinishEvent) {
        ensureTechTree(event.controller);
        Networks.unlockTech(event.controller, techA);
    })
    .build();

RecipeBuilder.newBuilder("network_demo_unlock_techb", machineB, 20)
    .addItemInput(<minecraft:gold_ingot> * 1)
    .addCheckHandler(function(event as RecipeCheckEvent) {
        if (!requireNetwork(event)) {
            return;
        }
        ensureTechTree(event.controller);
        if (!Networks.canUnlockTech(event.controller, techB)) {
            event.setFailed("need techA");
        }
    })
    .addFinishHandler(function(event as RecipeFinishEvent) {
        Networks.unlockTech(event.controller, techB);
    })
    .build();

RecipeBuilder.newBuilder("network_demo_require_techa", machineB, 20)
    .addItemInput(<minecraft:diamond> * 1)
    .addItemOutput(<minecraft:emerald> * 1)
    .addCheckHandler(function(event as RecipeCheckEvent) {
        if (!requireNetwork(event)) {
            return;
        }
        ensureTechTree(event.controller);
        if (!Networks.isTechUnlocked(event.controller, techA)) {
            event.setFailed("techA not unlocked");
        }
    })
    .build();

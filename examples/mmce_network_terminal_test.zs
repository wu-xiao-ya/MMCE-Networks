#loader crafttweaker reloadable
import mods.mmcenetworks.Networks;

import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.RecipeFinishEvent;
import mods.modularmachinery.FactoryRecipeFinishEvent;
import mods.modularmachinery.FactoryRecipeTickEvent;
import mods.modularmachinery.MMEvents;

// 把这里改成你自己的机器注册名。
val providerMachine = "networks-A";
val consumerMachine = "networks-B";
val basicFlag = "testTechBasicUnlocked";
val advancedFlag = "testTechAdvancedUnlocked";

// 终端卡片式显示配置。
// 这些是全局显示规则：打开任意网络终端时，会按这些规则读取当前网络里的同名 key。
Networks.clearTerminalValueDisplays();
Networks.setTerminalDisplayLayout("machine");
Networks.registerTerminalBar("testScore", "测试分数", "{value} / 100", 100);
Networks.registerTerminalStatus("networkOnline", "网络状态", "在线", "离线");
Networks.registerTerminalText("testMode", "当前模式", "{value}");
Networks.registerTerminalValue("testOwner", "写入来源", "{value}");
Networks.registerTerminalStatus(basicFlag, "基础科技", "已解锁", "未解锁");
Networks.registerTerminalStatus(advancedFlag, "高级科技", "已解锁", "未解锁");

function requireNetwork(event as RecipeCheckEvent) as bool {
    if (!Networks.hasNetwork(event.controller)) {
        event.setFailed("machine has no network");
        return false;
    }
    return true;
}

// 配方 1：
// providerMachine 吃 1 个圆石，给 testScore +10。
RecipeBuilder.newBuilder("network_test_add_score", providerMachine, 20)
    .addItemInput(<minecraft:cobblestone> * 1)
    .addRecipeTooltip(
        "测试用途：向网络数值页写入 testScore。",
        "效果：每完成 1 次，testScore +10。",
        "终端观察：数值页会出现 testScore / testOwner / testMode / networkOnline。"
    )
    .addFactoryFinishHandler(function(event as FactoryRecipeFinishEvent) {
        Networks.setString(event.controller, "testOwner", "terminal_test");
        Networks.setString(event.controller, "testMode", "score_added");
        Networks.setBoolean(event.controller, "networkOnline", true);
        Networks.defineTech(event.controller, "tech_basic");
        Networks.defineTech(event.controller, "tech_advanced");
        Networks.addTechPrerequisite(event.controller, "tech_advanced", "tech_basic");
        Networks.addInt(event.controller, "testScore", 10);
    })
    .build();

// 配方 2：
// providerMachine 吃 1 个红石，解锁基础科技 tech_basic。
RecipeBuilder.newBuilder("network_test_unlock_basic", providerMachine, 20)
    .addItemInput(<minecraft:redstone> * 1)
    .addRecipeTooltip(
        "测试用途：解锁基础科技 tech_basic。",
        "科技说明：tech_basic 是 tech_advanced 的前置科技。",
        "终端观察：科技页中 tech_basic 会变为已解锁。"
    )
    .addFactoryFinishHandler(function(event as FactoryRecipeFinishEvent) {
        Networks.setString(event.controller, "testOwner", "terminal_test");
        Networks.setString(event.controller, "testMode", "basic_unlocked");
        Networks.setBoolean(event.controller, "networkOnline", true);
        Networks.defineTech(event.controller, "tech_basic");
        Networks.defineTech(event.controller, "tech_advanced");
        Networks.addTechPrerequisite(event.controller, "tech_advanced", "tech_basic");
        Networks.setBoolean(event.controller, basicFlag, true);
        Networks.unlockTech(event.controller, "tech_basic");
    })
    .build();

// 配方 3：
// providerMachine 吃 1 个金锭，需要已解锁 tech_basic，完成后解锁 tech_advanced。
RecipeBuilder.newBuilder("network_test_unlock_advanced", providerMachine, 20)
    .addItemInput(<minecraft:gold_ingot> * 1)
    .addRecipeTooltip(
        "测试用途：解锁高级科技 tech_advanced。",
        "前置条件：必须先解锁 tech_basic。",
        "终端观察：科技页会显示 tech_advanced 依赖 tech_basic，并在完成后解锁。"
    )
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        if (!requireNetwork(event)) {
            return;
        }

        Networks.defineTech(event.controller, "tech_basic");
        Networks.defineTech(event.controller, "tech_advanced");
        Networks.addTechPrerequisite(event.controller, "tech_advanced", "tech_basic");
        if (!Networks.getBoolean(event.controller, basicFlag, false) && !Networks.isTechUnlocked(event.controller, "tech_basic")) {
            event.setFailed("need tech_basic");
        }
    })
    .addFactoryFinishHandler(function(event as FactoryRecipeFinishEvent) {
        Networks.setString(event.controller, "testMode", "advanced_unlocked");
        Networks.setBoolean(event.controller, advancedFlag, true);
        Networks.unlockTech(event.controller, "tech_advanced");
    })
    .build();

// 配方 4：
// consumerMachine 吃 1 个木板，需要 testScore >= 10，
// 完成后扣 10 分并输出 1 个铁锭。
RecipeBuilder.newBuilder("network_test_consume_score", consumerMachine, 20)
    .addItemInput(<minecraft:planks:0> * 1)
    .addItemOutput(<minecraft:iron_ingot> * 1)
    .addRecipeTooltip(
        "测试用途：验证网络数值消耗。",
        "前置条件：testScore >= 10。",
        "效果：完成后消耗 10 点 testScore，并产出 1 个铁锭。"
    )
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        if (!requireNetwork(event)) {
            return;
        }
        if (Networks.getInt(event.controller, "testScore", 0) < 10) {
            event.setFailed("need testScore >= 10");
        }
    })
    .addFactoryFinishHandler(function(event as FactoryRecipeFinishEvent) {
        Networks.tryConsumeInt(event.controller, "testScore", 10);
        Networks.setString(event.controller, "testMode", "score_consumed");
    })
    .build();

// 配方 5：
// consumerMachine 吃 1 个钻石，需要 tech_advanced 已解锁，
// 完成后输出 1 个绿宝石。
RecipeBuilder.newBuilder("network_test_require_advanced", consumerMachine, 20)
    .addItemInput(<minecraft:diamond> * 1)
    .addItemOutput(<minecraft:emerald> * 1)
    .addRecipeTooltip(
        "测试用途：验证科技门槛。",
        "前置条件：必须已解锁 tech_advanced。",
        "科技关系：tech_advanced 依赖 tech_basic。",
        "效果：满足条件后可把钻石转成绿宝石。"
    )
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        if (!requireNetwork(event)) {
            return;
        }

        Networks.defineTech(event.controller, "tech_basic");
        Networks.defineTech(event.controller, "tech_advanced");
        Networks.addTechPrerequisite(event.controller, "tech_advanced", "tech_basic");
        if (!Networks.getBoolean(event.controller, advancedFlag, false) && !Networks.isTechUnlocked(event.controller, "tech_advanced")) {
            event.setFailed("need tech_advanced");
        }
    })
    .build();

// 配方 6：
// providerMachine 吃 1 个青金石。
// 运行期间每个线程临时提供 10 点 compute，停止后会延迟回落。
RecipeBuilder.newBuilder("network_test_boost_compute", providerMachine, 20)
    .addItemInput(<minecraft:dye:4> * 1)
    .addRecipeTooltip(
        "测试用途：验证运行中算力供给。",
        "运行期间：每个活跃线程临时提供 10 点 compute。",
        "停止后不会立刻消失，会按 transientSupplyGraceTicks 延迟回落。",
        "多线程并行时应按线程数叠加。"
    )
    .addFactoryPreTickHandler(function(event as FactoryRecipeTickEvent) {
        Networks.pulseThreadSupply(event, "compute", 10);
    })
    .build();

// 配方 7：
// consumerMachine 吃 1 个石英，需要可用 compute >= 9。
// 默认会失败，另一个机器运行算力供给配方后才会成功。
RecipeBuilder.newBuilder("network_test_require_high_compute", consumerMachine, 20)
    .addItemInput(<minecraft:quartz> * 1)
    .addItemOutput(<minecraft:gold_nugget> * 1)
    .addRecipeTooltip(
        "测试用途：验证高算力门槛。",
        "前置条件：可用 compute >= 9。",
        "默认没有运行中供给，因此此配方初始应失败。",
        "先让另一个机器运行算力供给配方后，此配方才应成功。"
    )
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        if (!requireNetwork(event)) {
            return;
        }
        if (!Networks.canUse(event.controller, "compute", 9)) {
            event.setFailed("need compute >= 9");
        }
    })
    .build();

// 配方 8：
// consumerMachine 吃 1 个煤炭，在运行期间每个线程临时提供 5 点 compute。
// 多线程并行时应按线程数叠加。
RecipeBuilder.newBuilder("network_test_thread_compute_boost", consumerMachine, 60)
    .addItemInput(<minecraft:coal> * 1)
    .addRecipeTooltip(
        "测试用途：验证按线程叠加的瞬态算力供给。",
        "运行期间：每个活跃线程临时提供 5 点 compute。",
        "若同一机器并行 2 个线程同时运行，总增益应为 +10。",
        "停止后不会立刻消失，会按 transientSupplyGraceTicks 延迟回落。"
    )
    .addFactoryPreTickHandler(function(event as FactoryRecipeTickEvent) {
        Networks.pulseThreadSupply(event, "compute", 5);
    })
    .build();

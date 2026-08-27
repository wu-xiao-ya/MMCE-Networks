#loader crafttweaker reloadable
#modloaded modularmachinery
#modloaded mmcenetworks

import mods.mmcenetworks.Networks;

import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.FactoryRecipeFinishEvent;
import mods.modularmachinery.FactoryRecipeTickEvent;

// This file is a self-contained physical compute-topology test suite.
// The matrix machine is the existing demo tower; the other four machines
// are defined in machines/mmcen_network_examples/.
static MATRIX_MACHINE as string = "mmcen_demo_compute_tower";
static PROVIDER_MACHINE as string = "mmcen_test_compute_provider";
static WIRED_CONSUMER_MACHINE as string = "mmcen_test_compute_wired_consumer";
static WIRELESS_CONSUMER_MACHINE as string = "mmcen_test_compute_wireless_consumer";
static OVERLOAD_CONSUMER_MACHINE as string = "mmcen_test_compute_overload_consumer";

static MATRIX_THROUGHPUT as long = 128;
static PYLON_THROUGHPUT as long = 96;
static WORK_THROUGHPUT as long = 96;
static RESEARCH_THROUGHPUT as long = 96;
static DISTRIBUTOR_THROUGHPUT as long = 128;
static PROVIDER_OUTPUT as long = 96;
static WIRED_DEMAND as long = 24;
static WIRELESS_DEMAND as long = 40;
static OVERLOAD_DEMAND as long = 100;

static PYLON_LINE as string = "pylon_line";
static WORK_LINE as string = "work_line";
static RESEARCH_LINE as string = "research_line";
static MAIN_DISTRIBUTOR as string = "main_tower";

// Run once after binding the matrix controller to an MMCE N network.
RecipeBuilder.newBuilder("mmcen_test_configure_compute_topology", MATRIX_MACHINE, 20)
    .addItemInput(<minecraft:comparator> * 1)
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        requireNetwork(event);
    })
    .addFactoryFinishHandler(function(event as FactoryRecipeFinishEvent) {
        Networks.configureComputeMatrix(event.controller, MATRIX_THROUGHPUT, 0);
        Networks.configureComputeInterface(event.controller, PYLON_LINE, PYLON_THROUGHPUT, 4, 0, false);
        Networks.configureComputeInterface(event.controller, WORK_LINE, WORK_THROUGHPUT, 8, 0, false);
        Networks.configureComputeInterface(event.controller, RESEARCH_LINE, RESEARCH_THROUGHPUT, 8, 32, true);
        Networks.configureComputeDistributor(event.controller, MAIN_DISTRIBUTOR, DISTRIBUTOR_THROUGHPUT, 16, 16, 0);
    })
    .setParallelized(false)
    .build();

// 96 CU/t producer over a wired route.
RecipeBuilder.newBuilder("mmcen_test_compute_provider_run", PROVIDER_MACHINE, 200)
    .addItemInput(<minecraft:redstone_block> * 1)
    .addItemOutput(<minecraft:quartz> * 1)
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        requireNetwork(event);
    })
    .addFactoryPreTickHandler(function(event as FactoryRecipeTickEvent) {
        if (Networks.hasNetwork(event.controller)) {
            if (!Networks.hasComputeRoute(event.controller)) {
                Networks.bindComputeRoute(event.controller, PYLON_LINE, MAIN_DISTRIBUTOR, "wired");
            }
            Networks.reportCompute(event.controller, PROVIDER_OUTPUT, 0);
        }
    })
    .setParallelized(false)
    .build();

// 24 CU/t consumer over an entity-cable route.
RecipeBuilder.newBuilder("mmcen_test_compute_wired_consumer_run", WIRED_CONSUMER_MACHINE, 100)
    .addItemInput(<minecraft:coal> * 1)
    .addItemOutput(<minecraft:iron_ingot> * 1)
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        requireNetwork(event);
    })
    .addFactoryPreTickHandler(function(event as FactoryRecipeTickEvent) {
        if (Networks.hasNetwork(event.controller)) {
            if (!Networks.hasComputeRoute(event.controller)) {
                Networks.bindComputeRoute(event.controller, WORK_LINE, MAIN_DISTRIBUTOR, "wired");
            }
            Networks.reportCompute(event.controller, 0, WIRED_DEMAND);
            if (!Networks.isComputeDemandSatisfied(event.controller)) {
                event.preventProgressing("有线消费测试机需要完整 24 CU/t");
            }
        }
    })
    .setParallelized(false)
    .build();

// 40 CU/t consumer over the wireless interface's configured coverage.
RecipeBuilder.newBuilder("mmcen_test_compute_wireless_consumer_run", WIRELESS_CONSUMER_MACHINE, 100)
    .addItemInput(<minecraft:paper> * 1)
    .addItemOutput(<minecraft:gold_nugget> * 1)
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        requireNetwork(event);
    })
    .addFactoryPreTickHandler(function(event as FactoryRecipeTickEvent) {
        if (Networks.hasNetwork(event.controller)) {
            if (!Networks.hasComputeRoute(event.controller)) {
                Networks.bindComputeRoute(event.controller, RESEARCH_LINE, MAIN_DISTRIBUTOR, "wireless");
            }
            Networks.reportCompute(event.controller, 0, WIRELESS_DEMAND);
            if (!Networks.isComputeDemandSatisfied(event.controller)) {
                event.preventProgressing("无线消费测试机需要完整 40 CU/t，且必须处于无线覆盖范围");
            }
        }
    })
    .setParallelized(false)
    .build();

// 100 CU/t consumer on the 96 CU/t work line. Run this separately or
// together with the wired consumer to verify incomplete allocation.
RecipeBuilder.newBuilder("mmcen_test_compute_overload_consumer_run", OVERLOAD_CONSUMER_MACHINE, 100)
    .addItemInput(<minecraft:diamond> * 1)
    .addItemOutput(<minecraft:emerald> * 1)
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        requireNetwork(event);
    })
    .addFactoryPreTickHandler(function(event as FactoryRecipeTickEvent) {
        if (Networks.hasNetwork(event.controller)) {
            if (!Networks.hasComputeRoute(event.controller)) {
                Networks.bindComputeRoute(event.controller, WORK_LINE, MAIN_DISTRIBUTOR, "wired");
            }
            Networks.reportCompute(event.controller, 0, OVERLOAD_DEMAND);
            if (!Networks.isComputeDemandSatisfied(event.controller)) {
                event.preventProgressing("压力测试机需要完整 100 CU/t，当前有线接口吞吐量为 96 CU/t");
            }
        }
    })
    .setParallelized(false)
    .build();

function requireNetwork(event as RecipeCheckEvent) as bool {
    if (!Networks.hasNetwork(event.controller)) {
        event.setFailed("请先将控制器加入 MMCE N 网络");
        return false;
    }
    return true;
}

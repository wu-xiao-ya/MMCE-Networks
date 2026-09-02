#loader crafttweaker reloadable
#modloaded modularmachinery
#modloaded mmcenetworks

import mods.mmcenetworks.Networks;

import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.FactoryRecipeFinishEvent;

// One matrix structure accepts all four wired interface tiers. Replace the
// interface block in the same structure position to change the physical cap.
static MATRIX_MACHINE as string = "mmcen_demo_compute_tower";
static LINE_ID as string = "tier_test_line";
static DISTRIBUTOR_ID as string = "tier_test_distributor";

static MATRIX_THROUGHPUT as long = 4096;
static MACHINE_LIMIT as int = 0;
static LINE_MACHINE_LIMIT as int = 16;

// Run this once after the controller joins an MMCE N network.
RecipeBuilder.newBuilder("mmcen_test_configure_compute_port_tier_matrix", MATRIX_MACHINE, 20)
    .addItemInput(<minecraft:comparator> * 1)
    .addPostCheckHandler(function(event as RecipeCheckEvent) {
        requireNetwork(event);
    })
    .addFactoryFinishHandler(function(event as FactoryRecipeFinishEvent) {
        Networks.configureComputeMatrix(event.controller, MATRIX_THROUGHPUT, MACHINE_LIMIT);
        Networks.configureComputeInterface(
            event.controller, LINE_ID, MATRIX_THROUGHPUT, LINE_MACHINE_LIMIT, 0, false
        );
        Networks.configureComputeDistributor(
            event.controller, DISTRIBUTOR_ID, MATRIX_THROUGHPUT,
            LINE_MACHINE_LIMIT, LINE_MACHINE_LIMIT, 0
        );
    })
    .setParallelized(false)
    .build();

function requireNetwork(event as RecipeCheckEvent) as bool {
    if (!Networks.hasNetwork(event.controller)) {
        event.setFailed("请先将控制器加入 MMCE N 网络。");
        return false;
    }
    return true;
}

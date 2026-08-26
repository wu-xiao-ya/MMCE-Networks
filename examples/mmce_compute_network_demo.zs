import mods.mmcenetworks.Networks;

import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.RecipeFinishEvent;
import mods.modularmachinery.FactoryRecipeTickEvent;

// Replace these names with machines from the current pack.
val setupMachine = "machine_a";
val providerMachine = "machine_a";
val consumerMachine = "machine_b";

// The setup recipe configures persistent topology for the shared network.
RecipeBuilder.newBuilder("compute_network_setup", setupMachine, 10)
    .addItemInput(<minecraft:iron_ingot> * 1)
    .addFinishHandler(function(event as RecipeFinishEvent) {
        Networks.configureComputeMatrix(event.controller, 120, 0);
        Networks.configureComputeInterface(event.controller, "line_a", 100, 4, 0, false);
        Networks.configureComputeInterface(event.controller, "line_b", 80, 4, 32, true);
        Networks.configureComputeDistributor(event.controller, "tower_a", 100, 4, 4, 0);
        Networks.configureComputeDistributor(event.controller, "tower_b", 80, 4, 4, 0);
    })
    .build();

// This machine supplies 100 CU/t through line_a.
RecipeBuilder.newBuilder("compute_network_provider", providerMachine, 20)
    .addItemInput(<minecraft:redstone> * 1)
    .addCheckHandler(function(event as RecipeCheckEvent) {
        if (!Networks.hasNetwork(event.controller)) {
            event.setFailed("machine has no network");
        } else if (!Networks.hasComputeRoute(event.controller)
            && !Networks.bindComputeRoute(event.controller, "line_a", "tower_a", "wired")) {
            event.setFailed("could not bind provider compute route");
        }
    })
    .addFactoryPreTickHandler(function(event as FactoryRecipeTickEvent) {
        Networks.reportCompute(event.controller, 100, 0);
    })
    .build();

// This machine requests 80 CU/t through line_b. It receives power from the
// matrix even though its line is different from the provider line.
RecipeBuilder.newBuilder("compute_network_consumer", consumerMachine, 20)
    .addItemInput(<minecraft:coal> * 1)
    .addFactoryPreTickHandler(function(event as FactoryRecipeTickEvent) {
        Networks.reportCompute(event.controller, 0, 80);
        val allocated = Networks.getComputeAllocated(event.controller);
        val ready = Networks.isComputeDemandSatisfied(event.controller);
        Networks.setLong(event.controller, "compute_network_consumer_allocated", allocated);
        Networks.setBoolean(event.controller, "compute_network_consumer_ready", ready);
        if (!ready) {
            event.preventProgressing("need complete compute allocation: 80 CU/t");
        }
    })
    .addCheckHandler(function(event as RecipeCheckEvent) {
        if (!Networks.hasNetwork(event.controller)) {
            event.setFailed("machine has no network");
        } else if (!Networks.hasComputeRoute(event.controller)
            && !Networks.bindComputeRoute(event.controller, "line_b", "tower_b", "wireless")) {
            event.setFailed("could not bind consumer compute route");
        }
    })
    .build();

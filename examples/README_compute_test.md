# Compute Network Test

This example is a first vertical slice for the real-time compute network.
It does not use the persistent resource-pool usage API.

## Test flow

1. Copy `machines/mmcen_network_examples` into the MMCE machinery config directory if you want to use the bundled demo controllers.
2. Copy `mmce_compute_network_demo.zs` into the CraftTweaker scripts directory.
3. Replace the three machine names at the top of the script.
4. Bind the setup, provider, and consumer controllers to the same MMCE Networks network.
5. Run `compute_network_setup` once.
6. Bind the provider and consumer compute routes.
7. Run `compute_network_provider` and `compute_network_consumer` at the same time.
8. Inspect the shared value `compute_network_consumer_ready`.

The provider is bound to `line_a` and `tower_a`; the consumer is bound to
`line_b` and `tower_b`. Runtime reports no longer choose their own path. The
server resolves the persisted route before accepting CU/t.

The example demonstrates these first-version rules:

- Matrix throughput is 120 CU/t.
- Provider line throughput is 100 CU/t.
- Consumer line throughput is 80 CU/t.
- Interfaces and distributors record the configuring controller as their
  physical anchor.
- A node without a route cannot report supply or demand.
- A legacy report with forged interface or distributor IDs is rejected.
- Wireless routes require the node and interface to be in the same dimension
  and inside the configured coverage.
- Wired routes are explicit connections by default. Addons can register
  `ComputeWiredRouteValidator` to require a real cable graph.
- The consumer requires the complete 80 CU/t to be satisfied.
- A partial allocation does not count as a successful demand.
- Repeated reports from the same controller, interface, and distributor in one
  tick are added together. This allows MMCE factory threads to contribute
  independently without overwriting each other.
- Factory scripts can use the `FactoryRecipeEvent` overloads of
  `reportCompute`, `getComputeAllocated`, and
  `isComputeDemandSatisfied` to identify each MMCE factory thread separately.
  A machine still counts as one topology node, while its threads can receive
  partial allocation independently. For example, six threads requesting
  `24 CU/t` on a `96 CU/t` line allow four threads to progress and leave two
  waiting.
- Machine and distributor limits are counted from nodes reporting during the
  current tick, so unloaded or stopped machines do not remain registered.
- Supply, demand, allocation, and per-node results expire when they are not
  reported again on the next world tick.
- Runtime telemetry is appended to controller sync payloads for client GUI
  reads, but it is not written into the persistent network topology.

The topology configuration is persisted in the network shared data under the
versioned `_computeNetwork` tag. Runtime reports and allocations are cleared
after each world tick.

## CraftTweaker API

Topology:

- `configureComputeMatrix(controller, throughput, machineLimit)`
- `configureComputeInterface(controller, id, throughput, machineLimit, coverage, wireless)`
- `removeComputeInterface(controller, id)`
- `configureComputeDistributor(controller, id, throughput, machineLimit, bindingLimit, coverage)`
- `removeComputeDistributor(controller, id)`
- `bindComputeRoute(controller, interfaceId, distributorId, connectionType)`
- `unbindComputeRoute(controller)`
- `hasComputeRoute(controller)`
- `getComputeRouteStatus(controller)`

Runtime:

- `reportCompute(controller, cpuOutput, demand)`
- `reportCompute(factoryEvent, cpuOutput, demand)` (thread-scoped)
- `reportCompute(controller, interfaceId, distributorId, cpuOutput, demand)` (legacy validated overload)
- `getComputeAllocated(controller)`
- `getComputeAllocated(factoryEvent)` (thread-scoped)
- `isComputeDemandSatisfied(controller)`
- `isComputeDemandSatisfied(factoryEvent)` (thread-scoped)
- `getComputeNetworkSupply(controller)`
- `getComputeNetworkDemand(controller)`
- `getComputeNetworkAllocated(controller)`
- `getComputeEligibleNodes(controller)`
- `getComputeRejectedNodes(controller)`
- `getComputeSnapshot(controller)`

The network totals and per-controller allocation getters can be called from an
MMCE client GUI render handler. Their values come from ephemeral telemetry
synced with the controller rather than from a persistent compute cache.

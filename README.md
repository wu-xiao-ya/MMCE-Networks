# Modular Machinery: Community Edition Networks

语言 / Language: [中文](#中文) | [English](#english)

## 中文

MMCE Networks 是一个面向 Minecraft 1.12.2 / Modular Machinery: Community Edition 的附属模组，用来把多个 MMCE 控制器接入同一个“网络”。绑定到同一网络的机器可以共享公共数据、资源池、科技树状态，并通过游戏内网络终端查看这些信息。

这个模组适合用在需要“多机器协作”“跨机器变量”“资源容量/占用”“科技解锁”“任务式生产线”的整合包或剧情包里。

### 主要功能

- 网络绑定器：在游戏内创建网络、绑定控制器、复制网络、解绑控制器、查看绑定状态。
- 共享数据：通过 CraftTweaker 在同一网络中读写 `int`、`long`、`double`、`boolean`、`string` 和 NBT 数据。
- 资源池：定义网络容量、使用量、可用量，支持临时供给和按线程临时供给。
- 科技树：定义科技、前置科技、解锁/锁定科技，并在脚本中作为配方门槛。
- 网络终端 GUI：查看网络数据、资源池、科技树和网络列表。
- 终端卡片显示：创作者可以用脚本把共享数据渲染成普通数值、文本、进度条和状态灯。

### 运行环境

- Minecraft 1.12.2
- Forge / Cleanroom 1.12.2 环境
- Modular Machinery: Community Edition
- CraftTweaker 2，若需要脚本 API

开发环境使用 RetroFuturaGradle。构建时需要本地存在 MMCE 依赖 jar，当前工程在 `build.gradle` 中通过 `compileOnly files('..\\MMCEGE\\mmce.jar')` 引用。

### 构建

```powershell
.\gradlew.bat --no-daemon build --max-workers=1 --console=plain
```

构建产物位于：

```text
build/libs/
```

### 游戏内快速上手

1. 准备一个 MMCE 控制器。
2. 手持 `MMCE 网络绑定器`。
3. 潜行 + 右键空气：创建一个新网络，并把网络 ID 写入绑定器。
4. 右键 MMCE 控制器：把控制器绑定到绑定器中的网络。
5. 不潜行右键空气：打开当前网络的终端 GUI。
6. 潜行 + 鼠标滚轮：切换绑定器模式。

绑定器模式：

- `绑定`：把控制器绑定到当前绑定器网络；如果绑定器没有网络，会自动创建。
- `复制`：从已绑定控制器复制网络 ID 到绑定器。
- `解绑`：移除控制器网络绑定。
- `查看`：查看控制器当前绑定状态。

### CraftTweaker 基础用法

导入 API：

```zenscript
import mods.mmcenetworks.Networks;
```

在 MMCE 配方事件中，通常可以通过 `event.controller` 访问当前控制器。只有控制器已经绑定到网络时，下面这些数据才会写入对应网络。

```zenscript
if (!Networks.hasNetwork(event.controller)) {
    event.setFailed("machine has no network");
    return;
}

Networks.setInt(event.controller, "score", 20);
Networks.setString(event.controller, "mode", "running");
Networks.setBoolean(event.controller, "online", true);

val score = Networks.getInt(event.controller, "score", 0);
```

常用共享数据 API：

- `hasNetwork(controller)`
- `getNetworkId(controller)`
- `contains(controller, key)`
- `get(controller, key)` / `set(controller, key, data)`
- `getInt/getLong/getDouble/getBoolean/getString`
- `setInt/setLong/setDouble/setBoolean/setString`
- `addInt(controller, key, delta)`
- `tryConsumeInt(controller, key, amount)`
- `remove(controller, key)`

### 资源池教程

资源池适合表达算力、电力、流体容量、线路负载等“容量/占用”关系。

```zenscript
Networks.setSupply(event.controller, "compute", 10);

if (!Networks.canUse(event.controller, "compute", 3)) {
    event.setFailed("need compute >= 3");
    return;
}

if (!Networks.trySetUsage(event.controller, "compute", 3)) {
    event.setFailed("compute busy");
    return;
}

Networks.clearUsage(event.controller, "compute");
```

运行时临时供给：

```zenscript
Networks.pulseSupply(event.controller, "compute", 5);
```

按线程临时供给，适合多线程机器：

```zenscript
Networks.pulseThreadSupply(event, "compute", 5);
```

### 实时算力拓扑

实时 `CU/t` 不使用资源池缓存。机器即使属于同一个普通 MMCE N 网络，也必须额外绑定算力路由才能上报供给或需求。

```zenscript
Networks.configureComputeMatrix(event.controller, 128, 0);
Networks.configureComputeInterface(event.controller, "work_line", 96, 8, 0, false);
Networks.configureComputeDistributor(event.controller, "main_tower", 128, 16, 16, 2);

Networks.bindComputeRoute(event.controller, "work_line", "main_tower", "wired");
Networks.reportCompute(event.controller, 0, 24);

if (!Networks.isComputeDemandSatisfied(event.controller)) {
    event.preventProgressing("需要完整 24 CU/t");
}
```

- `wired`：使用持久化显式路由；兼容模组可注册 `ComputeWiredRouteValidator` 接入真实线缆图。
- `wireless`：服务端按接口锚点、维度和覆盖范围校验。
- `reportCompute(controller, cpuOutput, demand)`：由服务端解析已绑定线路。
- 旧五参数上报仍可兼容，但线路 ID 必须与服务端路由完全一致。
- 低于完整需求时 `isComputeDemandSatisfied` 返回 `false`。

### 科技树教程

科技树适合做整合包进度、生产线阶段、任务解锁。

```zenscript
Networks.defineTech(event.controller, "tech_basic");
Networks.defineTech(event.controller, "tech_advanced");
Networks.addTechPrerequisite(event.controller, "tech_advanced", "tech_basic");

if (!Networks.canUnlockTech(event.controller, "tech_advanced")) {
    event.setFailed("need tech_basic");
    return;
}

Networks.unlockTech(event.controller, "tech_basic");
```

### 网络终端卡片教程

网络终端读取当前网络共享数据，并按全局注册的显示规则渲染右侧信息区。

```zenscript
Networks.clearTerminalValueDisplays();
Networks.setTerminalDisplayLayout("machine");

Networks.registerTerminalValue("power", "功率", "{value} RF/t");
Networks.registerTerminalBar("energy", "能源", "{value} RF", 100000);
Networks.registerTerminalStatus("online", "状态", "运行中", "离线");
Networks.registerTerminalText("hint", "提示", "{value}");
```

写入网络数据：

```zenscript
Networks.setInt(event.controller, "power", 320);
Networks.setInt(event.controller, "energy", 45000);
Networks.setBoolean(event.controller, "online", true);
Networks.setString(event.controller, "hint", "反应堆运行稳定");
```

可用布局：`list`、`dashboard`、`story`、`machine`。

可用卡片：

- `registerTerminalValue(key, name, template)`：普通键值。
- `registerTerminalText(key, name, template)`：说明文本。
- `registerTerminalBar(key, name, template, max)`：进度条。
- `registerTerminalStatus(key, name, trueText, falseText)`：状态灯。
- `registerTerminalCard(key, name, template, type, options)`：底层通用入口。

注意：显示规则是全局的；实际显示值来自玩家当前打开的网络。不同网络可以显示同一套 key，但各自拥有独立数据。

### 示例脚本

示例位于 [`examples/`](examples/)：

- [`mmce_network_demo.zs`](examples/mmce_network_demo.zs)：基础网络变量、资源池和科技树示例。
- [`mmce_network_terminal_test.zs`](examples/mmce_network_terminal_test.zs)：终端 GUI 和卡片式显示测试脚本。
- [`README_terminal_test.md`](examples/README_terminal_test.md)：终端测试步骤说明。

使用示例前请先把脚本中的机器注册名改成你自己的 MMCE 机器名，然后放入 CraftTweaker scripts 目录。

### 许可证

代码部分使用 MIT License，见 [`LICENSE`](LICENSE)。

材质、模型、GUI 美术、图标、粒子等非代码素材不包含在 MIT 授权内，使用、修改、转载或移植前需要获得额外授权。详见 [`ASSETS_LICENSE.md`](ASSETS_LICENSE.md)。

## English

MMCE Networks is an addon for Minecraft 1.12.2 and Modular Machinery: Community Edition. It lets multiple MMCE controllers join the same network. Machines bound to one network can share public data, resource pools, tech-tree state, and inspect that information through an in-game network terminal.

The mod is intended for modpacks that need multi-machine cooperation, cross-machine variables, resource capacity and usage, technology unlocks, or quest-like production chains.

### Features

- Network Linker: create networks, bind controllers, copy network IDs, unbind controllers, and inspect bindings in-game.
- Shared data: read and write `int`, `long`, `double`, `boolean`, `string`, and NBT data through CraftTweaker.
- Resource pools: define capacity, usage, and availability, including transient supply and per-thread transient supply.
- Tech tree: define technologies, prerequisites, unlocks, and locks for recipe gates.
- Network terminal GUI: inspect network data, resource pools, tech tree, and network list.
- Terminal cards: render shared data as values, text, progress bars, and status lights.

### Requirements

- Minecraft 1.12.2
- Forge / Cleanroom 1.12.2 environment
- Modular Machinery: Community Edition
- CraftTweaker 2 if scripting APIs are needed

The development environment uses RetroFuturaGradle. Building currently expects a local MMCE dependency jar referenced by `compileOnly files('..\\MMCEGE\\mmce.jar')` in `build.gradle`.

### Build

```powershell
.\gradlew.bat --no-daemon build --max-workers=1 --console=plain
```

Build outputs are generated in:

```text
build/libs/
```

### In-Game Quick Start

1. Prepare an MMCE controller.
2. Hold the `MMCE Network Linker`.
3. Sneak + right-click air to create a new network and store its ID in the linker.
4. Right-click an MMCE controller to bind it to the linker's network.
5. Right-click air without sneaking to open the terminal GUI for the current network.
6. Sneak + mouse wheel to switch linker modes.

Linker modes:

- `Bind`: bind a controller to the current linker network; if the linker has no network, one is created automatically.
- `Copy`: copy the network ID from an already-bound controller into the linker.
- `Unbind`: remove a controller's network binding.
- `Inspect`: inspect the controller's current binding.

### CraftTweaker Basics

Import the API:

```zenscript
import mods.mmcenetworks.Networks;
```

In MMCE recipe events, `event.controller` usually refers to the current controller. Data is written to the controller's bound network.

```zenscript
if (!Networks.hasNetwork(event.controller)) {
    event.setFailed("machine has no network");
    return;
}

Networks.setInt(event.controller, "score", 20);
Networks.setString(event.controller, "mode", "running");
Networks.setBoolean(event.controller, "online", true);

val score = Networks.getInt(event.controller, "score", 0);
```

Common shared-data APIs:

- `hasNetwork(controller)`
- `getNetworkId(controller)`
- `contains(controller, key)`
- `get(controller, key)` / `set(controller, key, data)`
- `getInt/getLong/getDouble/getBoolean/getString`
- `setInt/setLong/setDouble/setBoolean/setString`
- `addInt(controller, key, delta)`
- `tryConsumeInt(controller, key, amount)`
- `remove(controller, key)`

### Resource Pool Tutorial

Resource pools are useful for compute power, energy, fluid capacity, line load, or any capacity/usage relationship.

```zenscript
Networks.setSupply(event.controller, "compute", 10);

if (!Networks.canUse(event.controller, "compute", 3)) {
    event.setFailed("need compute >= 3");
    return;
}

if (!Networks.trySetUsage(event.controller, "compute", 3)) {
    event.setFailed("compute busy");
    return;
}

Networks.clearUsage(event.controller, "compute");
```

Transient runtime supply:

```zenscript
Networks.pulseSupply(event.controller, "compute", 5);
```

Per-thread transient supply for multithreaded machines:

```zenscript
Networks.pulseThreadSupply(event, "compute", 5);
```

### Real-Time Compute Topology

Real-time `CU/t` does not use a persistent resource-pool cache. Controllers in
the same ordinary MMCE N network must still bind a compute route before they
can report supply or demand.

```zenscript
Networks.configureComputeMatrix(event.controller, 128, 0);
Networks.configureComputeInterface(event.controller, "work_line", 96, 8, 0, false);
Networks.configureComputeDistributor(event.controller, "main_tower", 128, 16, 16, 2);

Networks.bindComputeRoute(event.controller, "work_line", "main_tower", "wired");
Networks.reportCompute(event.controller, 0, 24);

if (!Networks.isComputeDemandSatisfied(event.controller)) {
    event.preventProgressing("requires complete 24 CU/t");
}
```

- `wired` uses a persisted explicit route. An addon can register
  `ComputeWiredRouteValidator` to enforce a physical cable graph.
- `wireless` is validated by interface anchor, dimension, and coverage.
- `reportCompute(controller, cpuOutput, demand)` resolves the bound path on the server.
- `reportCompute(factoryEvent, cpuOutput, demand)` reports the current MMCE
  factory thread separately, so a line can run the threads it can afford while
  the remaining threads wait.
- The legacy five-argument overload remains available, but its path IDs must
  exactly match the server route.
- `isComputeDemandSatisfied` stays false below the complete demand.
- Use the `FactoryRecipeEvent` overload of `isComputeDemandSatisfied` inside a
  factory pre-tick handler when partial per-thread allocation is desired.

### Tech Tree Tutorial

Tech trees are useful for modpack progression, production stages, and quest gates.

```zenscript
Networks.defineTech(event.controller, "tech_basic");
Networks.defineTech(event.controller, "tech_advanced");
Networks.addTechPrerequisite(event.controller, "tech_advanced", "tech_basic");

if (!Networks.canUnlockTech(event.controller, "tech_advanced")) {
    event.setFailed("need tech_basic");
    return;
}

Networks.unlockTech(event.controller, "tech_basic");
```

### Terminal Card Tutorial

The terminal reads the current network's shared data and renders the right-side information area using globally registered display rules.

```zenscript
Networks.clearTerminalValueDisplays();
Networks.setTerminalDisplayLayout("machine");

Networks.registerTerminalValue("power", "Power", "{value} RF/t");
Networks.registerTerminalBar("energy", "Energy", "{value} RF", 100000);
Networks.registerTerminalStatus("online", "Status", "Running", "Offline");
Networks.registerTerminalText("hint", "Hint", "{value}");
```

Write network data:

```zenscript
Networks.setInt(event.controller, "power", 320);
Networks.setInt(event.controller, "energy", 45000);
Networks.setBoolean(event.controller, "online", true);
Networks.setString(event.controller, "hint", "Reactor is stable");
```

Available layouts: `list`, `dashboard`, `story`, `machine`.

Available cards:

- `registerTerminalValue(key, name, template)`: normal key-value display.
- `registerTerminalText(key, name, template)`: text description.
- `registerTerminalBar(key, name, template, max)`: progress bar.
- `registerTerminalStatus(key, name, trueText, falseText)`: status light.
- `registerTerminalCard(key, name, template, type, options)`: low-level generic entry point.

Display rules are global, but values are read from the network currently opened by the player. Different networks can use the same keys while keeping separate data.

### Example Scripts

Examples are in [`examples/`](examples/):

- [`mmce_network_demo.zs`](examples/mmce_network_demo.zs): basic shared variables, resource pools, and tech-tree examples.
- [`mmce_network_terminal_test.zs`](examples/mmce_network_terminal_test.zs): terminal GUI and terminal-card test script.
- [`README_terminal_test.md`](examples/README_terminal_test.md): terminal test walkthrough.

Before using the examples, replace the machine registry names with your own MMCE machine names, then place the scripts into the CraftTweaker scripts directory.

### License

Source code is licensed under the MIT License. See [`LICENSE`](LICENSE).

Textures, models, GUI artwork, icons, particles, and other non-code assets are not covered by the MIT license. Reuse, modification, redistribution, or porting requires additional authorization. See [`ASSETS_LICENSE.md`](ASSETS_LICENSE.md).

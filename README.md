# Modular Machinery: Community Edition Networks

MMCE Networks 是一个面向 Minecraft 1.12.2 / Modular Machinery: Community Edition 的附属模组，用来把多个 MMCE 控制器接入同一个“网络”。绑定到同一网络的机器可以共享公共数据、资源池、科技树状态，并通过游戏内网络终端查看这些信息。

这个模组适合用在需要“多机器协作”“跨机器变量”“资源容量/占用”“科技解锁”“任务式生产线”的整合包或剧情包里。

## 主要功能

- 网络绑定器：在游戏内创建网络、绑定控制器、复制网络、解绑控制器、查看绑定状态。
- 共享数据：通过 CraftTweaker 在同一网络中读写 `int`、`long`、`double`、`boolean`、`string` 和 NBT 数据。
- 资源池：定义网络容量、使用量、可用量，支持临时供给和按线程临时供给。
- 科技树：定义科技、前置科技、解锁/锁定科技，并在脚本中作为配方门槛。
- 网络终端 GUI：查看网络数据、资源池、科技树和网络列表。
- 终端卡片显示：创作者可以用脚本把共享数据渲染成普通数值、文本、进度条和状态灯。

## 运行环境

- Minecraft 1.12.2
- Forge / Cleanroom 1.12.2 环境
- Modular Machinery: Community Edition
- CraftTweaker 2，若需要脚本 API

开发环境使用 RetroFuturaGradle。构建时需要本地存在 MMCE 依赖 jar，当前工程在 `build.gradle` 中通过 `compileOnly files('..\\MMCEGE\\mmce.jar')` 引用。

## 构建

```powershell
.\gradlew.bat --no-daemon build --max-workers=1 --console=plain
```

构建产物位于：

```text
build/libs/
```

## 游戏内快速上手

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

## CraftTweaker 基础用法

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

## 资源池教程

资源池适合表达算力、电力、流体容量、线路负载等“容量/占用”关系。

```zenscript
// 提供 10 点 compute。
Networks.setSupply(event.controller, "compute", 10);

// 检查是否至少有 3 点可用 compute。
if (!Networks.canUse(event.controller, "compute", 3)) {
    event.setFailed("need compute >= 3");
    return;
}

// 占用 3 点 compute，配方结束后释放。
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

## 科技树教程

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

常用科技 API：

- `defineTech(controller, techId)`
- `removeTech(controller, techId)`
- `addTechPrerequisite(controller, techId, prerequisiteId)`
- `removeTechPrerequisite(controller, techId, prerequisiteId)`
- `hasTech(controller, techId)`
- `isTechUnlocked(controller, techId)`
- `canUnlockTech(controller, techId)`
- `unlockTech(controller, techId)`
- `lockTech(controller, techId)`
- `getTechTree(controller)`
- `getTech(controller, techId)`

## 网络终端卡片教程

网络终端读取当前网络共享数据，并按全局注册的显示规则渲染右侧信息区。

注册显示规则：

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

可用布局：

- `list`：普通列表。
- `dashboard`：卡片之间留间距，适合少量核心指标。
- `story`：偏文本说明。
- `machine`：带轻微卡片底色，适合机器面板。

可用卡片：

- `registerTerminalValue(key, name, template)`：普通键值。
- `registerTerminalText(key, name, template)`：说明文本。
- `registerTerminalBar(key, name, template, max)`：进度条。
- `registerTerminalStatus(key, name, trueText, falseText)`：状态灯。
- `registerTerminalCard(key, name, template, type, options)`：底层通用入口。

注意：显示规则是全局的；实际显示值来自玩家当前打开的网络。不同网络可以显示同一套 key，但各自拥有独立数据。

## 示例脚本

示例位于 [`examples/`](examples/)：

- [`mmce_network_demo.zs`](examples/mmce_network_demo.zs)：基础网络变量、资源池和科技树示例。
- [`mmce_network_terminal_test.zs`](examples/mmce_network_terminal_test.zs)：终端 GUI 和卡片式显示测试脚本。
- [`README_terminal_test.md`](examples/README_terminal_test.md)：终端测试步骤说明。

使用示例前请先把脚本中的机器注册名改成你自己的 MMCE 机器名，然后放入 CraftTweaker scripts 目录。

## 许可证

当前仓库尚未提供明确许可证文件。若需要在整合包、二次开发或公开分发中使用，请先联系作者确认授权范围。

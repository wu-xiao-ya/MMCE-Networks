# MMCE Networks Terminal Test

语言 / Language: [中文](#中文) | [English](#english)

## 中文

`mmce_network_terminal_test.zs` 是一组专门给网络终端 GUI 验证用的测试配方。每个测试配方都额外带了 JEI 说明文字，会在配方页里解释用途、科技前置和测试目标。

使用前先改这两个机器名：

- `providerMachine = "machine_a"`
- `consumerMachine = "machine_b"`

建议测试顺序：

1. 用网络绑定器把两个控制器绑到同一网络。
2. 打开 `providerMachine`：放入 `圆石`，执行 `network_test_add_score`，数值页应出现 `testScore`。
3. 继续看数值页：常驻会写入 `testOwner`、`testMode`、`networkOnline`。
4. 切到资源池页：应看到 `compute` 和 `fluid` 两个资源池。
5. 回到 `providerMachine`：放入 `红石`，执行 `network_test_unlock_basic`，解锁 `tech_basic`。
6. 再放入 `金锭`，执行 `network_test_unlock_advanced`，解锁 `tech_advanced`。
7. 切到科技页：应看到 `tech_basic` 和 `tech_advanced`，且后者依赖前者。
8. 打开 `consumerMachine`：放入 `木板`，执行 `network_test_consume_score`，需要 `testScore >= 10`，成功后产出 `铁锭`。
9. 继续在 `consumerMachine`：放入 `钻石`，执行 `network_test_require_advanced`，需要 `tech_advanced` 已解锁，成功后产出 `绿宝石`。

如果只是想看终端三页有无内容，这套脚本已经够用，不需要额外复杂配方。

### 卡片式终端显示

`mmce_network_terminal_test.zs` 顶部已经注册了一组卡片式终端规则：

```zenscript
Networks.clearTerminalValueDisplays();
Networks.setTerminalDisplayLayout("machine");
Networks.registerTerminalBar("testScore", "测试分数", "{value} / 100", 100);
Networks.registerTerminalStatus("networkOnline", "网络状态", "在线", "离线");
Networks.registerTerminalText("testMode", "当前模式", "{value}");
Networks.registerTerminalValue("testOwner", "写入来源", "{value}");
```

这些注册是全局显示规则，不是单独创建网络。实际显示内容来自当前打开网络里的共享数据。

也就是说：

1. 游戏内用网络绑定器创建/绑定网络。
2. 配方运行时用 `Networks.setInt/setString/setBoolean` 写入数据。
3. 打开终端后，GUI 会读取当前网络的同名 key，并按卡片规则显示。

可用布局：`list`、`dashboard`、`story`、`machine`。

可用卡片：

- `registerTerminalValue(key, name, template)`：普通键值。
- `registerTerminalText(key, name, template)`：说明文本。
- `registerTerminalBar(key, name, template, max)`：进度条。
- `registerTerminalStatus(key, name, trueText, falseText)`：状态灯。

## English

`mmce_network_terminal_test.zs` is a set of test recipes for verifying the network terminal GUI. Each recipe includes JEI tooltips that explain its purpose, tech prerequisites, and test target.

Before use, change these two machine names:

- `providerMachine = "machine_a"`
- `consumerMachine = "machine_b"`

Suggested test order:

1. Use the Network Linker to bind both controllers to the same network.
2. Open `providerMachine`: insert `cobblestone` and run `network_test_add_score`; the values page should show `testScore`.
3. Keep checking the values page: `testOwner`, `testMode`, and `networkOnline` are written persistently.
4. Switch to the resource-pool page: `compute` and `fluid` should appear.
5. Return to `providerMachine`: insert `redstone` and run `network_test_unlock_basic` to unlock `tech_basic`.
6. Insert `gold ingot` and run `network_test_unlock_advanced` to unlock `tech_advanced`.
7. Switch to the tech page: `tech_basic` and `tech_advanced` should appear, with `tech_advanced` depending on `tech_basic`.
8. Open `consumerMachine`: insert `planks` and run `network_test_consume_score`; it requires `testScore >= 10` and outputs an `iron ingot` on success.
9. Continue in `consumerMachine`: insert `diamond` and run `network_test_require_advanced`; it requires `tech_advanced` and outputs an `emerald` on success.

If you only want to check whether the terminal pages contain data, this script is enough. No extra complex recipes are required.

### Terminal Card Display

`mmce_network_terminal_test.zs` registers terminal-card display rules near the top:

```zenscript
Networks.clearTerminalValueDisplays();
Networks.setTerminalDisplayLayout("machine");
Networks.registerTerminalBar("testScore", "Test Score", "{value} / 100", 100);
Networks.registerTerminalStatus("networkOnline", "Network Status", "Online", "Offline");
Networks.registerTerminalText("testMode", "Current Mode", "{value}");
Networks.registerTerminalValue("testOwner", "Writer", "{value}");
```

These are global display rules, not separate network creation. The displayed values come from the shared data of the network currently opened by the player.

In short:

1. Create or bind a network in-game with the Network Linker.
2. During recipe execution, write data with `Networks.setInt/setString/setBoolean`.
3. Open the terminal; the GUI reads the same keys from the current network and renders them as cards.

Available layouts: `list`, `dashboard`, `story`, `machine`.

Available cards:

- `registerTerminalValue(key, name, template)`: normal key-value display.
- `registerTerminalText(key, name, template)`: text description.
- `registerTerminalBar(key, name, template, max)`: progress bar.
- `registerTerminalStatus(key, name, trueText, falseText)`: status light.

# MMCE Networks Examples

语言 / Language: [中文](#中文) | [English](#english)

## 中文

这里放的是 CraftTweaker 示例脚本和测试说明。使用前请先把脚本里的机器注册名改成你整合包中的 MMCE 机器名。

### 文件说明

- `mmce_network_demo.zs`：基础示例，演示网络变量、资源池、算力占用和科技树。
- `mmce_network_terminal_test.zs`：终端 GUI 测试示例，包含卡片式终端显示配置。
- `README_terminal_test.md`：终端测试脚本的详细测试步骤。
- `mmce_compute_network_demo.zs`：实时算力矩阵、接口、分发塔和路由示例。
- `README_compute_test.md`：实时算力拓扑的详细测试步骤。
- `machines/mmcen_network_examples/`：六台可直接复制到测试包的 MMCE 联调机器。

### 快速测试流程

1. 把 `.zs` 示例复制到 CraftTweaker scripts 目录。
2. 修改脚本顶部的机器注册名。
3. 重载脚本或重启游戏。
4. 用网络绑定器创建网络，并把相关 MMCE 控制器绑定到同一网络。
5. 运行示例配方，再打开网络终端查看共享数据、资源池、科技树和卡片式显示。

更多说明见仓库根目录 `README.md`。

## English

This directory contains CraftTweaker example scripts and test notes. Before use, replace the machine registry names in the scripts with your own MMCE machine names.

### Files

- `mmce_network_demo.zs`: basic example for network variables, resource pools, compute usage, and tech tree.
- `mmce_network_terminal_test.zs`: terminal GUI test example with terminal-card display configuration.
- `README_terminal_test.md`: detailed walkthrough for the terminal test script.
- `mmce_compute_network_demo.zs`: real-time compute matrix, interface, distributor, and route example.
- `README_compute_test.md`: detailed walkthrough for the real-time compute topology.
- `machines/mmcen_network_examples/`: six MMCE integration-test machine definitions.

### Quick Test Flow

1. Copy the `.zs` examples into the CraftTweaker scripts directory.
2. Edit the machine registry names near the top of the scripts.
3. Reload scripts or restart the game.
4. Use the Network Linker to create a network, then bind the relevant MMCE controllers to the same network.
5. Run the example recipes, then open the network terminal to inspect shared data, resource pools, tech tree, and terminal-card display.

For more information, see the root `README.md`.

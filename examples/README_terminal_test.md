`mmce_network_terminal_test.zs` 是一组专门给网络终端 GUI 验证用的测试配方。
每个测试配方都额外带了 JEI 说明文字，会在配方页里解释用途、科技前置和测试目标。

使用前先改这两个机器名：
- `providerMachine = "machine_a"`
- `consumerMachine = "machine_b"`

建议测试顺序：
1. 用网络绑定器把两个控制器绑到同一网络。
2. 打开 `providerMachine`：
   放入 `圆石`，执行 `network_test_add_score`，数值页应出现 `testScore`。
3. 继续看数值页：
   常驻会写入 `testOwner`、`testMode`、`networkOnline`。
4. 切到资源池页：
   应看到 `compute` 和 `fluid` 两个资源池。
   `providerMachine` 提供 `compute=8`、`fluid=16`。
   `consumerMachine` 在线时会占用 `compute=3`、`fluid=5`。
5. 回到 `providerMachine`：
   放入 `红石`，执行 `network_test_unlock_basic`，解锁 `tech_basic`。
6. 再放入 `金锭`，执行 `network_test_unlock_advanced`，解锁 `tech_advanced`。
7. 切到科技页：
   应看到 `tech_basic` 和 `tech_advanced`，且后者依赖前者。
8. 打开 `consumerMachine`：
   放入 `木板`，执行 `network_test_consume_score`，需要 `testScore >= 10`，成功后产出 `铁锭`。
9. 继续在 `consumerMachine`：
   放入 `钻石`，执行 `network_test_require_advanced`，需要 `tech_advanced` 已解锁，成功后产出 `绿宝石`。

如果只是想看终端三页有无内容，这套脚本已经够用，不需要额外复杂配方。

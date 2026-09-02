# 有线算力端口等级测试

这个测试使用一个统一的 `mmcen_demo_compute_tower`。矩阵结构的同一个位置允许四档有线端口，替换端口不会改变机器注册名，也不会创建第二个矩阵。

端口等级：

- `wired_compute_interface`：基础级，96 CU/t
- `wired_compute_interface_reinforced`：强化级，192 CU/t
- `wired_compute_interface_advanced`：高级级，384 CU/t
- `wired_compute_interface_elite`：极限级，768 CU/t

实际有线线路吞吐量取以下限制中的较小值：

```text
实际线路吞吐量 =
min(CraftTweaker 线路配置, 结构中有线端口的物理吞吐量总和)
```

## 测试步骤

1. 将 `machines/mmcen_network_examples` 复制到 `config/modularmachinery/machinery`。
2. 将 `mmce_compute_port_tier_test.zs` 复制到 CraftTweaker `scripts` 目录。
3. 成型一个 `mmcen_demo_compute_tower`，并把控制器加入目标 MMCE N 网络。
4. 运行 `mmcen_test_configure_compute_port_tier_matrix` 一次，发布矩阵、线路和分支交换节点配置。
5. 用实体算力线缆连接算力供给机或消费机。
6. 依次把矩阵结构中同一端口位置的有线端口替换为四个等级，观察网络终端中的实际可分配吞吐量。

分支交换节点是可选的交换/分流节点。矩阵吞吐量是网络级上限；同一 MMCE N 网络中如果存在多个独立矩阵结构，它们的有效矩阵吞吐量和机器上限会按模块累加，线路可以连接到其中任意一个可达矩阵端点。

# 算力拓扑测试机器

这组机器用于验证 MMCE N 的实体线路、无线骨干、分发节点吞吐和完整算力需求。

## 文件

- `machines/mmcen_network_examples/mmcen_test_compute_provider.json`
  - 算力供给测试机，运行时提供 96 CU/t。
- `machines/mmcen_network_examples/mmcen_test_compute_wired_consumer.json`
  - 有线算力消费测试机，持续需要 24 CU/t。
- `machines/mmcen_network_examples/mmcen_test_compute_wireless_consumer.json`
  - 无线算力消费测试机，持续需要 40 CU/t。
- `machines/mmcen_network_examples/mmcen_test_compute_overload_consumer.json`
  - 压力测试机，持续需要 100 CU/t；工作接口只有 96 CU/t，因此默认不能完成。
- `mmce_compute_topology_test.zs`
  - 配置矩阵、绑定路线并注册四台测试机的测试配方。

## 安装

1. 将新增 JSON 复制到 `config/modularmachinery/machinery/mmcen_network_examples/`。
2. 将 `mmce_compute_topology_test.zs` 复制到 CraftTweaker 的 `scripts/` 目录。
3. 重载脚本或重启游戏。

## 测试顺序

1. 成型一台 `mmcen_demo_compute_tower`，它是一个包含算力矩阵、分发节点、有线接入端和无线接入端的演示结构；用 MMCE N 网络绑定器将其控制器加入网络。
2. 在矩阵塔中运行 `mmcen_test_configure_compute_topology` 一次。
3. 成型四台测试机。每台的 `compute_port` 应自动归属本机控制器。
4. 用实体算力线缆把供给机的机器算力端口连接到矩阵结构的有线接入端，再连接到分发节点和算力矩阵。
5. 启动供给机和有线消费机，确认两台机器只有在实体线路完整时推进。
6. 将无线消费机放入无线接入端 32 格覆盖范围内。它不需要机器到接入端的实体线缆，但无线接入端到分发节点/算力矩阵必须有线连通。
7. 启动压力测试机，确认工作线路只有 96 CU/t 时不会因得到部分算力而推进。
8. 拆除任意一段线路，确认端点仍保留网络 ID，但对应算力路由变为离线。

压力测试机可以与有线消费机同时运行，用于验证同一线路或分发节点的吞吐分配；也可以单独运行，用于验证 100 CU/t 超过 96 CU/t 时不会以部分算力推进。

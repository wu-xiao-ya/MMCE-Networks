package com.mmce.networks.common.compute.transport;

public enum ComputeEndpointType {
    MACHINE_PORT(
        "machine_port",
        "\u673a\u5668\u63a5\u53e3",
        "\u673a\u5668\u7b97\u529b\u7aef\u53e3",
        "\u653e\u5728\u4f9b\u7ed9\u3001\u6d88\u8d39\u6216\u7814\u7a76 MMCE \u673a\u5668\u5185\u3002"
            + "\u5b83\u53ea\u8868\u793a\u8fd9\u53f0\u673a\u5668\u4e0e\u7b97\u529b\u7f51\u7edc\u7684\u8fde\u63a5\u4f4d\u7f6e\uff0c\u4e0d\u662f\u7f51\u7edc\u6838\u5fc3\u3002"
    ),
    WIRED_INTERFACE(
        "wired_interface",
        "\u77e9\u9635\u63a5\u5165",
        "\u6709\u7ebf\u63a5\u5165\u7aef\u53e3",
        "\u653e\u5728\u7b97\u529b\u77e9\u9635\u7ed3\u6784\u5185\uff0c\u7528\u6765\u63a5\u5165\u5b9e\u4f53\u7b97\u529b\u7ebf\u7f06\u3002"
            + "\u5b83\u4e0d\u4ea7\u751f\u7b97\u529b\uff0c\u53ea\u63d0\u4f9b\u8be5\u63a5\u5165\u53e3\u7684\u7269\u7406 CU/t \u4e0a\u9650\u3002"
    ),
    WIRELESS_INTERFACE(
        "wireless_interface",
        "\u77e9\u9635\u63a5\u5165",
        "\u65e0\u7ebf\u63a5\u5165\u7aef\u53e3",
        "\u653e\u5728\u7b97\u529b\u77e9\u9635\u7ed3\u6784\u5185\uff0c\u7528\u65e0\u7ebf\u8986\u76d6\u66ff\u4ee3\u673a\u5668\u5230\u77e9\u9635\u7684\u90e8\u5206\u7ebf\u7f06\u3002"
            + "\u5b83\u4ecd\u7136\u9700\u8981\u7528\u5b9e\u4f53\u7ebf\u7f06\u4e0a\u8054\u5230\u77e9\u9635\u9aa8\u5e72\u3002"
    ),
    DISTRIBUTOR(
        "distributor",
        "\u5206\u652f\u8bbe\u5907",
        "\u5206\u652f\u4ea4\u6362\u8282\u70b9",
        "\u653e\u5728\u77e9\u9635\u7ed3\u6784\u6216\u72ec\u7acb\u5206\u652f\u7ed3\u6784\u5185\uff0c\u50cf\u4ea4\u6362\u673a\u4e00\u6837\u7ec4\u7ec7\u7ebf\u8def\u3002"
            + "\u5b83\u53ef\u4ee5\u4e3a\u4e00\u4e2a\u5206\u652f\u5355\u72ec\u9650\u5236 CU/t\u3001\u673a\u5668\u6570\u548c\u7ed1\u5b9a\u6570\uff0c\u4f46\u4e0d\u4ea7\u751f\u7b97\u529b\u3002"
    ),
    MATRIX(
        "matrix",
        "\u7f51\u7edc\u6838\u5fc3",
        "\u77e9\u9635\u6838\u5fc3\u7aef\u53e3",
        "\u653e\u5728\u7b97\u529b\u77e9\u9635\u7ed3\u6784\u5185\uff0c\u4ee3\u8868\u8fd9\u53f0 MMCE \u673a\u5668\u63d0\u4f9b\u7684\u7f51\u7edc\u9aa8\u5e72\u3002"
            + "\u5b83\u4e0d\u662f\u7b97\u529b\u6765\u6e90\uff0c\u4e5f\u4e0d\u66ff\u4ee3\u77e9\u9635\u63a5\u5165\u7aef\u53e3\u3002"
    );

    private final String id;
    private final String category;
    private final String displayName;
    private final String description;

    ComputeEndpointType(
        final String id,
        final String category,
        final String displayName,
        final String description
    ) {
        this.id = id;
        this.category = category;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }
}

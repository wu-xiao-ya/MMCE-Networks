package com.mmce.networks.common.compute.transport;

public enum ComputeEndpointType {
    MACHINE_PORT(
        "machine_port",
        "\u673a\u5668\u7b97\u529b\u7aef\u53e3",
        "\u653e\u5728 MMCE \u673a\u5668\u7ed3\u6784\u5185\uff0c\u8fd9\u53f0\u673a\u5668\u4ece\u6b64\u7aef\u53e3\u63d0\u4f9b\u6216\u6d88\u8d39\u7b97\u529b\u3002"
    ),
    WIRED_INTERFACE(
        "wired_interface",
        "\u6709\u7ebf\u63a5\u5165\u7aef",
        "\u7528\u5b9e\u4f53\u7b97\u529b\u7ebf\u7f06\u8fde\u63a5\u673a\u5668\u3002"
    ),
    WIRELESS_INTERFACE(
        "wireless_interface",
        "\u65e0\u7ebf\u63a5\u5165\u7aef",
        "\u63d0\u4f9b\u65e0\u7ebf\u8986\u76d6\uff0c\u4f46\u81ea\u8eab\u4ecd\u9700\u7528\u5b9e\u4f53\u7ebf\u7f06\u8fde\u56de\u77e9\u9635\u6216\u5206\u53d1\u8282\u70b9\u3002"
    ),
    DISTRIBUTOR(
        "distributor",
        "\u5206\u53d1\u8282\u70b9",
        "\u53ef\u9009\u7684\u4ea4\u6362\u8282\u70b9\uff0c\u9650\u5236\u5206\u652f\u7684\u7b97\u529b\u541e\u5410\u548c\u673a\u5668\u6570\u91cf\u3002"
    ),
    MATRIX(
        "matrix",
        "\u7b97\u529b\u77e9\u9635",
        "\u7f51\u7edc\u6838\u5fc3\uff0c\u6240\u6709\u7b97\u529b\u5206\u652f\u6700\u7ec8\u90fd\u8981\u8fde\u56de\u8fd9\u91cc\u3002"
    );

    private final String id;
    private final String displayName;
    private final String description;

    ComputeEndpointType(
        final String id,
        final String displayName,
        final String description
    ) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

package com.mmce.networks.common.init;

import com.mmce.networks.common.block.BlockComputeCable;
import com.mmce.networks.common.block.BlockComputeEndpoint;
import com.mmce.networks.common.compute.transport.ComputeEndpointType;
import net.minecraft.block.Block;

public final class ModBlocks {
    public static final Block COMPUTE_CABLE = new BlockComputeCable();
    public static final Block COMPUTE_PORT = new BlockComputeEndpoint(
        "compute_port",
        ComputeEndpointType.MACHINE_PORT
    );
    public static final Block WIRED_COMPUTE_INTERFACE = new BlockComputeEndpoint(
        "wired_compute_interface",
        ComputeEndpointType.WIRED_INTERFACE,
        96L,
        "\u57fa\u7840\u7ea7"
    );
    public static final Block WIRED_COMPUTE_INTERFACE_REINFORCED = new BlockComputeEndpoint(
        "wired_compute_interface_reinforced",
        ComputeEndpointType.WIRED_INTERFACE,
        192L,
        "\u5f3a\u5316\u7ea7"
    );
    public static final Block WIRED_COMPUTE_INTERFACE_ADVANCED = new BlockComputeEndpoint(
        "wired_compute_interface_advanced",
        ComputeEndpointType.WIRED_INTERFACE,
        384L,
        "\u9ad8\u7ea7"
    );
    public static final Block WIRED_COMPUTE_INTERFACE_ELITE = new BlockComputeEndpoint(
        "wired_compute_interface_elite",
        ComputeEndpointType.WIRED_INTERFACE,
        768L,
        "\u6781\u9650\u7ea7"
    );
    public static final Block WIRELESS_COMPUTE_INTERFACE = new BlockComputeEndpoint(
        "wireless_compute_interface",
        ComputeEndpointType.WIRELESS_INTERFACE
    );
    public static final Block COMPUTE_DISTRIBUTOR = new BlockComputeEndpoint(
        "compute_distributor",
        ComputeEndpointType.DISTRIBUTOR
    );
    public static final Block COMPUTE_MATRIX_PORT = new BlockComputeEndpoint(
        "compute_matrix_port",
        ComputeEndpointType.MATRIX
    );

    public static final Block[] ALL = {
        COMPUTE_CABLE,
        COMPUTE_PORT,
        WIRED_COMPUTE_INTERFACE,
        WIRED_COMPUTE_INTERFACE_REINFORCED,
        WIRED_COMPUTE_INTERFACE_ADVANCED,
        WIRED_COMPUTE_INTERFACE_ELITE,
        WIRELESS_COMPUTE_INTERFACE,
        COMPUTE_DISTRIBUTOR,
        COMPUTE_MATRIX_PORT
    };

    private ModBlocks() {
    }
}

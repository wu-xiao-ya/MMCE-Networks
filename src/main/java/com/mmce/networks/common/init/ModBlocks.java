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
        ComputeEndpointType.WIRED_INTERFACE
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
        WIRELESS_COMPUTE_INTERFACE,
        COMPUTE_DISTRIBUTOR,
        COMPUTE_MATRIX_PORT
    };

    private ModBlocks() {
    }
}

package com.mmce.networks.common.compute.transport;

public enum ComputeEndpointType {
    MACHINE_PORT("machine_port"),
    WIRED_INTERFACE("wired_interface"),
    WIRELESS_INTERFACE("wireless_interface"),
    DISTRIBUTOR("distributor"),
    MATRIX("matrix");

    private final String id;

    ComputeEndpointType(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

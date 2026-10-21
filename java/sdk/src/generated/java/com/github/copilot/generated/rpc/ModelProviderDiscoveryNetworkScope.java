/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Network reach an adapter may use during discovery.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelProviderDiscoveryNetworkScope {
    /** The {@code none} variant. */
    NONE("none"),
    /** The {@code loopbackOnly} variant. */
    LOOPBACKONLY("loopbackOnly"),
    /** The {@code configuredEndpointOnly} variant. */
    CONFIGUREDENDPOINTONLY("configuredEndpointOnly"),
    /** The {@code localNetwork} variant. */
    LOCALNETWORK("localNetwork"),
    /** The {@code internet} variant. */
    INTERNET("internet");

    private final String value;
    ModelProviderDiscoveryNetworkScope(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelProviderDiscoveryNetworkScope fromValue(String value) {
        for (ModelProviderDiscoveryNetworkScope v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelProviderDiscoveryNetworkScope value: " + value);
    }
}

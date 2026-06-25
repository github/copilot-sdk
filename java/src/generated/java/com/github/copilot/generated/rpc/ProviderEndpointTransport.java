/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Transport to be used for provider requests.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderEndpointTransport {
    /** The {@code http} variant. */
    HTTP("http"),
    /** The {@code websockets} variant. */
    WEBSOCKETS("websockets");

    private final String value;
    ProviderEndpointTransport(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderEndpointTransport fromValue(String value) {
        for (ProviderEndpointTransport v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderEndpointTransport value: " + value);
    }
}

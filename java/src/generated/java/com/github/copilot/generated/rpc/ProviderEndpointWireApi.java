/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Wire API to be used, when required for the provider type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderEndpointWireApi {
    /** The {@code completions} variant. */
    COMPLETIONS("completions"),
    /** The {@code responses} variant. */
    RESPONSES("responses");

    private final String value;
    ProviderEndpointWireApi(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderEndpointWireApi fromValue(String value) {
        for (ProviderEndpointWireApi v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderEndpointWireApi value: " + value);
    }
}

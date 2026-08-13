/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Provider family. Matches the `type` field of a BYOK provider config.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderEndpointType {
    /** The {@code openai} variant. */
    OPENAI("openai"),
    /** The {@code azure} variant. */
    AZURE("azure"),
    /** The {@code anthropic} variant. */
    ANTHROPIC("anthropic");

    private final String value;
    ProviderEndpointType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderEndpointType fromValue(String value) {
        for (ProviderEndpointType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderEndpointType value: " + value);
    }
}

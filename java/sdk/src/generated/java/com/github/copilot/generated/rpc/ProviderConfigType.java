/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Provider type. Defaults to "openai" for generic OpenAI-compatible APIs.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderConfigType {
    /** The {@code openai} variant. */
    OPENAI("openai"),
    /** The {@code azure} variant. */
    AZURE("azure"),
    /** The {@code anthropic} variant. */
    ANTHROPIC("anthropic");

    private final String value;
    ProviderConfigType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderConfigType fromValue(String value) {
        for (ProviderConfigType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderConfigType value: " + value);
    }
}

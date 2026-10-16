/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * The product serving the model, reported in telemetry as `model_provider`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderConfigModelProvider {
    /** The {@code openai} variant. */
    OPENAI("openai"),
    /** The {@code anthropic} variant. */
    ANTHROPIC("anthropic"),
    /** The {@code azure_openai} variant. */
    AZURE_OPENAI("azure_openai"),
    /** The {@code ollama} variant. */
    OLLAMA("ollama"),
    /** The {@code lm_studio} variant. */
    LM_STUDIO("lm_studio"),
    /** The {@code foundry_local} variant. */
    FOUNDRY_LOCAL("foundry_local"),
    /** The {@code llama_cpp} variant. */
    LLAMA_CPP("llama_cpp");

    private final String value;
    ProviderConfigModelProvider(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderConfigModelProvider fromValue(String value) {
        for (ProviderConfigModelProvider v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderConfigModelProvider value: " + value);
    }
}

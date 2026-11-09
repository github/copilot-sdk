/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Provider families supported by saved configuration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SavedProviderConfigurationType {
    /** The {@code openai} variant. */
    OPENAI("openai"),
    /** The {@code azure} variant. */
    AZURE("azure"),
    /** The {@code anthropic} variant. */
    ANTHROPIC("anthropic"),
    /** The {@code local_model} variant. */
    LOCAL_MODEL("local_model"),
    /** The {@code winml} variant. */
    WINML("winml");

    private final String value;
    SavedProviderConfigurationType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SavedProviderConfigurationType fromValue(String value) {
        for (SavedProviderConfigurationType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SavedProviderConfigurationType value: " + value);
    }
}

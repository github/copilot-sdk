/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Kind of component that supplied a provider adapter or row. Attribution does not confer authority.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelProviderProvenanceSource {
    /** The {@code builtIn} variant. */
    BUILTIN("builtIn"),
    /** The {@code configured} variant. */
    CONFIGURED("configured"),
    /** The {@code extension} variant. */
    EXTENSION("extension"),
    /** The {@code custom} variant. */
    CUSTOM("custom");

    private final String value;
    ModelProviderProvenanceSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelProviderProvenanceSource fromValue(String value) {
        for (ModelProviderProvenanceSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelProviderProvenanceSource value: " + value);
    }
}

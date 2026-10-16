/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether a planned configuration entry is new or already present in the session registry.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelProviderConfigurationDisposition {
    /** The {@code create} variant. */
    CREATE("create"),
    /** The {@code alreadyConfigured} variant. */
    ALREADYCONFIGURED("alreadyConfigured");

    private final String value;
    ModelProviderConfigurationDisposition(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelProviderConfigurationDisposition fromValue(String value) {
        for (ModelProviderConfigurationDisposition v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelProviderConfigurationDisposition value: " + value);
    }
}

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * When the runtime may run an adapter without an explicit user action.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelProviderAutomaticDiscoveryMode {
    /** The {@code automatic} variant. */
    AUTOMATIC("automatic"),
    /** The {@code configuredOnly} variant. */
    CONFIGUREDONLY("configuredOnly"),
    /** The {@code explicit} variant. */
    EXPLICIT("explicit");

    private final String value;
    ModelProviderAutomaticDiscoveryMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelProviderAutomaticDiscoveryMode fromValue(String value) {
        for (ModelProviderAutomaticDiscoveryMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelProviderAutomaticDiscoveryMode value: " + value);
    }
}
